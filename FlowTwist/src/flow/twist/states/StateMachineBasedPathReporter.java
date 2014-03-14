package flow.twist.states;

import static flow.twist.trackable.Zero.ZERO;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.reporter.IfdsReporter;
import flow.twist.reporter.DelayingReporter;
import flow.twist.reporter.Report;
import flow.twist.states.StateCache.ContextStateCache;
import flow.twist.states.StateCache.StateMetadataWrapper;
import flow.twist.states.StateCache.StatePopNode;
import flow.twist.states.StateCache.StatePushNode;
import flow.twist.states.StateCache.StateSinkNode;
import flow.twist.trackable.ReturnEdgeTaint;
import flow.twist.trackable.Trackable;
import flow.twist.util.ImmutableLinkedHashSet;

/**
 * 
 * Does only work correctly if used within a {@link DelayingReporter}, because
 * it assumes that reported trackables will not change after the time they are
 * reported.
 */
public class StateMachineBasedPathReporter implements IfdsReporter {

	private final Set<Path> validPaths = Sets.newHashSet();
	private Stopwatch reportWatch;
	private StateCache globalStateCache = new StateCache();
	private int reportsProcessed = 0;

	public StateCache getStateCache() {
		return globalStateCache;
	}

	@Override
	public void analysisFinished() {
		if (reportWatch != null)
			System.out.println("Building states: " + reportWatch.stop());

		Stopwatch watch = new Stopwatch().start();
		collapseReturnStates();
		System.out.println("Collapsing return nodes: " + watch.stop());

		watch = new Stopwatch().start();
		buildPathsFromStates();
		System.out.println("Matching paths: " + watch.stop());
	}

	private void collapseReturnStates() {
		for (ContextStateCache stateCache : globalStateCache.getAll()) {
			Set<StatePopNode> worklist = Sets.newHashSet(stateCache.getAllPopStates());
			while (!worklist.isEmpty()) {
				collapseState(worklist.iterator().next(), worklist);
			}
		}
	}

	private void collapseState(StatePopNode current, Set<StatePopNode> worklist) {
		for (StateTransition incTrans : current.getIncoming()) {
			if (incTrans.getSource() instanceof StatePopNode && incTrans.getSource() != current) {
				collapseState((StatePopNode) incTrans.getSource(), worklist);
				return;
			}
		}

		worklist.remove(current);
		Set<StateNode> previousStates = Sets.newHashSet();
		for (StateTransition incTrans : current.getIncoming()) {
			StateNode previousState = incTrans.getSource();
			if (previousState == current)
				continue;

			previousStates.add(previousState);
			for (StateTransition newPrevTrans : previousState.getIncoming()) {
				buildCombinedTransitions(newPrevTrans, incTrans, current.getOutgoing());
			}
		}

		current.removeAllConnectionsRecursively();
	}

	private void buildCombinedTransitions(StateTransition first, StateTransition middle, List<StateTransition> allLast) {
		for (StateTransition last : allLast) {
			fj.data.List<PathElement> currentPath = AutomataMatcher.appendPath(last.getPath(), middle);
			currentPath = AutomataMatcher.appendPath(currentPath, first);
			StateTransition transition = new StateTransition(first.condition, first.isPushOnStack(), currentPath, first.connectingUnit);
			first.getSource().addOutgoingTransition(transition);
			last.getTarget().addIncomingTransition(transition);
		}
	}

	private void buildPathsFromStates() {
		AutomataMatcher matcher = new AutomataMatcher(globalStateCache);
		validPaths.addAll(matcher.getValidPaths());
	}

	public Set<Path> getValidPaths() {
		return validPaths;
	}

	@Override
	public void reportTrackable(Report report) {
		if (reportWatch == null)
			reportWatch = new Stopwatch().start();

		Set<Trackable> uniqueTrackablesSeen = Sets.newIdentityHashSet();
		ContextStateCache stateCache = globalStateCache.get(report.context);
		LinkedList<StateWorklistItem> workQueue = Lists.newLinkedList();
		StateNode startNode = stateCache.getOrCreateStartState(report.context.icfg.getMethodOf(report.targetUnit));

		workQueue.add(new StateWorklistItem(null, null, new Predecessor(report.targetUnit, report.trackable), new StateTransitionBuilder(report.context,
				startNode, true, report.context.icfg.getMethodOf(report.targetUnit), 0
		/*
		 * TODO set to 1 , and remove handling in updatePath
		 */)));
		int processed = 0;

		while (!workQueue.isEmpty()) {

			if (workQueue.getLast().unit != null && report.context.icfg.getMethodOf(workQueue.getLast().unit).toString().contains("interpretLoop")) {
				workQueue.removeLast();
				continue;
			}

			uniqueTrackablesSeen.add(workQueue.getLast().trackable);
			processed++;

			if (processed % 100000 == 0)
				System.out.println("Reports processed: " + reportsProcessed + "; Current report: Processed work items: "
						+ uniqueTrackablesSeen.size() + " / " + processed + " - WorkQueue size: " + workQueue.size());
			WorklistItemWorker worker = new WorklistItemWorker(stateCache, workQueue.removeLast());
			worker.start();
			worker.updateQueue(workQueue);
		}

		reportsProcessed++;
	}

	private static class WorklistItemWorker {

		private StateWorklistItem currentWork;
		private AnalysisContext context;
		private ContextStateCache stateCache;
		private boolean createPredecessingWorkItems = true;
		private StateTransitionBuilder newTransitionBuilder;
		private ImmutableLinkedHashSet<PathElement> newPath;
		private ImmutableLinkedHashSet<PathElement> updatedPath;

		public WorklistItemWorker(ContextStateCache stateCache, StateWorklistItem currentWork) {
			this.context = stateCache.context;
			this.stateCache = stateCache;
			this.currentWork = currentWork;
		}

		public void start() {
			setTransitionCondition();
			createUpdatedPath();
			if (isPushEdge()) {
				processPushEdge();
			} else if (isPopEdge()) {
				processPopEdge();
			} else {
				processInterproceduralEdge();
			}
			if (isAtSink()) {
				processSink();
			}
		}

		public void updateQueue(List<StateWorklistItem> queue) {
			if (!createPredecessingWorkItems)
				return;

			Set<Predecessor> workPredecessors = findUnequalPredecessors(currentWork.predecessor.trackable);
			for (Predecessor predecessor : workPredecessors) {
				queue.add(new StateWorklistItem(currentWork.predecessor.trackable, currentWork.predecessor.connectingUnit, predecessor, newPath,
						newTransitionBuilder));
			}
		}

		private void setTransitionCondition() {
			if (currentWork.trackable instanceof ReturnEdgeTaint) {
				currentWork.stateTransitionBuilder = currentWork.stateTransitionBuilder.withCondition(context.icfg
						.getMethodOf(currentWork.predecessor.connectingUnit));
			}
		}

		private boolean isPushEdge() {
			return currentWork.predecessor.trackable instanceof ReturnEdgeTaint;
		}

		private void processPushEdge() {
			StateMetadataWrapper<StatePushNode> pushNode = stateCache.getOrCreatePushState((ReturnEdgeTaint) currentWork.predecessor.trackable);

			ReturnEdgeTaint taint = (ReturnEdgeTaint) currentWork.predecessor.trackable;
			ImmutableLinkedHashSet<PathElement> path = updatedPath
					.add(new PathElement(taint.callSite, taint, currentWork.predecessor.connectingUnit));
			StateTransition transition = currentWork.stateTransitionBuilder.build(path.asList(), currentWork.predecessor.connectingUnit);
			pushNode.get().addIncomingTransition(transition);

			if (!pushNode.wasCreated())
				createPredecessingWorkItems = false;

			newPath = ImmutableLinkedHashSet.empty();
			newTransitionBuilder = new StateTransitionBuilder(context, pushNode.get(), true, 1);
		}

		private boolean isPopEdge() {
			if (currentWork.unit == null)
				return false;

			SootMethod toMethod = context.icfg.getMethodOf(currentWork.predecessor.connectingUnit);
			for (Predecessor pred : findUnequalPredecessors(currentWork.predecessor.trackable)) {
				SootMethod fromMethod = context.icfg.getMethodOf(pred.connectingUnit);
				if (!toMethod.equals(fromMethod))
					return true;
			}
			return false;
		}

		private void processPopEdge() {
			StateMetadataWrapper<StatePopNode> node = stateCache.getOrCreatePopState(currentWork.trackable);

			StateTransition transition = currentWork.stateTransitionBuilder.build(updatedPath.asList(), currentWork.predecessor.connectingUnit);
			node.get().addIncomingTransition(transition);

			if (!node.wasCreated())
				createPredecessingWorkItems = false;

			newTransitionBuilder = new StateTransitionBuilder(context, node.get(), false, 1);
			newPath = ImmutableLinkedHashSet.empty();
		}

		private void processInterproceduralEdge() {
			newPath = updatedPath;
			newTransitionBuilder = currentWork.stateTransitionBuilder;
		}

		private boolean isAtSink() {
			return currentWork.predecessor.trackable == ZERO;
		}

		private void processSink() {
			StateSinkNode node = stateCache.getSinkState(currentWork.predecessor.connectingUnit);
			StateWorklistItem sinkItem = new StateWorklistItem(currentWork.predecessor.trackable, currentWork.predecessor.connectingUnit, null,
					newPath, newTransitionBuilder);
			StateTransition transition = sinkItem.buildTransition();
			node.addIncomingTransition(transition);
			createPredecessingWorkItems = false;
		}

		private void createUpdatedPath() {
			if (currentWork.trackable == null) {
				updatedPath = currentWork.currentPath;
				return;// start worklist item
			}

			PathElement pathElement = new PathElement(currentWork.predecessor.connectingUnit, currentWork.trackable, currentWork.unit);
			if (currentWork.currentPath.contains(pathElement))
				createPredecessingWorkItems = false; // loop

			updatedPath = currentWork.currentPath.add(pathElement);
		}

		private Set<Predecessor> findUnequalPredecessors(Trackable startPoint) {
			Set<Predecessor> results = Sets.newHashSet();
			Set<Trackable> visitedTrackables = Sets.newIdentityHashSet();
			List<Trackable> worklist = Lists.newLinkedList();
			worklist.add(startPoint);

			while (!worklist.isEmpty()) {
				Trackable current = worklist.remove(0);
				for (Trackable neighbor : current.getSelfAndNeighbors()) {
					if (neighbor.predecessor == null)
						continue;

					if (neighbor.predecessor.equals(startPoint) && neighbor.predecessor.getClass() == startPoint.getClass()) {
						if (!visitedTrackables.add(neighbor.predecessor))
							continue; // recursive path
						worklist.add(neighbor.predecessor);
					} else
						results.add(new Predecessor(neighbor.sourceUnit, neighbor.predecessor));
				}
			}
			return results;
		}
	}

	private static class StateWorklistItem {
		private final Unit unit;
		private final Trackable trackable;
		private final Predecessor predecessor;
		private final ImmutableLinkedHashSet<PathElement> currentPath;
		private StateTransitionBuilder stateTransitionBuilder;

		public StateWorklistItem(Trackable trackable, Unit unit, Predecessor predecessor, StateTransitionBuilder stateTransitionBuilder) {
			this.trackable = trackable;
			this.unit = unit;
			this.predecessor = predecessor;
			this.currentPath = ImmutableLinkedHashSet.empty();
			this.stateTransitionBuilder = stateTransitionBuilder;
		}

		public StateWorklistItem(Trackable trackable, Unit unit, Predecessor predecessor, ImmutableLinkedHashSet<PathElement> currentPath,
				StateTransitionBuilder stateTransitionBuilder) {
			this.trackable = trackable;
			this.unit = unit;
			this.predecessor = predecessor;
			this.currentPath = currentPath;
			this.stateTransitionBuilder = stateTransitionBuilder;
		}

		public StateTransition buildTransition() {
			return stateTransitionBuilder.build(currentPath.asList(), null);
		}
	}

	private static class StateTransitionBuilder {
		private StateNode sourceNode;
		private boolean isPushOnStack;
		private AnalysisContext context;
		public final SootMethod condition;
		private final int skipFirstPathElements;

		private StateTransitionBuilder(AnalysisContext context, StateNode sourceNode, boolean isPushOnStack, int skipFirstPathElements) {
			this.context = context;
			this.sourceNode = sourceNode;
			this.isPushOnStack = isPushOnStack;
			this.skipFirstPathElements = skipFirstPathElements;
			condition = null;
		}

		private StateTransitionBuilder(AnalysisContext context, StateNode sourceNode, boolean isPushOnStack, SootMethod condition,
				int skipFirstPathElements) {
			this.context = context;
			this.sourceNode = sourceNode;
			this.isPushOnStack = isPushOnStack;
			this.condition = condition;
			this.skipFirstPathElements = skipFirstPathElements;
		}

		public StateTransition build(fj.data.List<PathElement> currentPath, Unit connectingUnit) {
			currentPath = currentPath.reverse().drop(skipFirstPathElements).reverse();
			StateTransition transition = new StateTransition(condition, isPushOnStack, currentPath, connectingUnit);
			sourceNode.addOutgoingTransition(transition);
			return transition;
		}

		public StateTransitionBuilder withCondition(SootMethod condition) {
			return new StateTransitionBuilder(context, sourceNode, isPushOnStack, condition, skipFirstPathElements);
		}
	}

	static class Predecessor {
		public final Unit connectingUnit;
		public final Trackable trackable;

		public Predecessor(Unit connectingUnit, Trackable trackable) {
			this.connectingUnit = connectingUnit;
			this.trackable = trackable;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((connectingUnit == null) ? 0 : connectingUnit.hashCode());
			result = prime * result + ((trackable == null) ? 0 : System.identityHashCode(trackable));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Predecessor other = (Predecessor) obj;
			if (connectingUnit == null) {
				if (other.connectingUnit != null)
					return false;
			} else if (!connectingUnit.equals(other.connectingUnit))
				return false;
			if (trackable == null) {
				if (other.trackable != null)
					return false;
			} else if (trackable != other.trackable)
				return false;
			return true;
		}
	}
}
