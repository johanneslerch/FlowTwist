package flow.twist.transformer.path;

import static flow.twist.trackable.Zero.ZERO;

import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.reporter.Report;
import flow.twist.trackable.PopFromStack;
import flow.twist.trackable.PushOnStack;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;
import flow.twist.util.ImmutableHashSet;
import flow.twist.util.ImmutableIdentityHashSet;

public class PathBuilder {

	private FirstIntroductionSelectorStrategy introductionStrategy;
	private Set<Path> currentResults;
	private List<WorklistItem> currentWorklist;
	private Report currentReport;

	public PathBuilder(FirstIntroductionSelectorStrategy introductionStrategy) {
		this.introductionStrategy = introductionStrategy;
	}

	public Set<Path> createPaths(Report report) {
		currentReport = report;
		currentResults = Sets.newHashSet();
		currentWorklist = initializeWorklist(report);
		while (!currentWorklist.isEmpty()) {
			processWorklistItem(currentWorklist.remove(0));
		}
		return currentResults;
	}

	protected void processWorklistItem(WorklistItem current) {
		for (Trackable t : introductionStrategy.firstIntroductionOf(current.getFirstElement().trackable.predecessor)) {
			if (t == ZERO) {
				addResultingPath(current);
			} else {
				WorklistItemBuilder itemBuilder = new WorklistItemBuilder(current, t);
				if (itemBuilder.isValid())
					currentWorklist.add(itemBuilder.create());
			}
		}
	}

	protected void addResultingPath(WorklistItem current) {
		currentResults.add(new Path(currentReport.context, current.pathElements, current.callStack, current.getFirstElement().from));
	}

	protected List<WorklistItem> initializeWorklist(Report report) {
		List<WorklistItem> worklist = Lists.newLinkedList();
		for (Trackable t : introductionStrategy.firstIntroductionOf(report.trackable)) {
			WorklistItemBuilder itemBuilder = new WorklistItemBuilder(t, report.targetUnit);
			if (itemBuilder.isValid())
				worklist.add(itemBuilder.create());
		}
		return worklist;
	}

	protected Set<SootMethod> getInitialDeclaration(Unit callSite) {
		return AnalysisUtil.getInitialDeclaration(((Stmt) callSite).getInvokeExpr().getMethod());
	}

	private static class WorklistItem {

		private fj.data.List<ImmutableIdentityHashSet<Trackable>> trackables;
		private fj.data.List<PathElement> pathElements;
		private fj.data.List<Object> callStack;
		private ImmutableHashSet<SootMethod> calledHierarchyMethods;

		public WorklistItem(fj.data.List<ImmutableIdentityHashSet<Trackable>> trackables, fj.data.List<PathElement> pathElements,
				fj.data.List<Object> callStack, ImmutableHashSet<SootMethod> calledHierarchyMethods) {
			this.trackables = trackables;
			this.pathElements = pathElements;
			this.callStack = callStack;
			this.calledHierarchyMethods = calledHierarchyMethods;
		}

		public PathElement getFirstElement() {
			return pathElements.head();
		}
	}

	private class WorklistItemBuilder {

		private boolean valid;
		private fj.data.List<ImmutableIdentityHashSet<Trackable>> trackables;
		private fj.data.List<PathElement> pathElements;
		private fj.data.List<Object> callStack;
		private ImmutableHashSet<SootMethod> calledHierarchyMethods;

		private WorklistItemBuilder(Trackable trackable, Unit targetUnit) {
			trackables = fj.data.List.<ImmutableIdentityHashSet<Trackable>> single(ImmutableIdentityHashSet.<Trackable> empty());
			pathElements = fj.data.List.<PathElement> nil();
			callStack = fj.data.List.<Object> nil();
			calledHierarchyMethods = ImmutableHashSet.<SootMethod> empty();
			valid = add(trackable, targetUnit);
		}

		private WorklistItemBuilder(WorklistItem item, Trackable trackable) {
			trackables = item.trackables;
			pathElements = item.pathElements;
			callStack = item.callStack;
			calledHierarchyMethods = item.calledHierarchyMethods;
			valid = add(trackable, item.getFirstElement().from);
		}

		public boolean isValid() {
			return valid;
		}

		private boolean add(Trackable trackable, Unit targetUnit) {
			if (!tryUpdateTrackables(trackable))
				return false;
			if (!tryUpdateCallStack(trackable))
				return false;
			if (!tryUpdateCalledHierarchyMethods(trackable))
				return false;
			updatePathElements(trackable, targetUnit);
			return true;
		}

		public WorklistItem create() {
			if (!valid)
				throw new IllegalStateException();
			return new WorklistItem(trackables, pathElements, callStack, calledHierarchyMethods);
		}

		private void updatePathElements(Trackable trackable, Unit targetUnit) {
			pathElements = pathElements.cons(new PathElement(trackable.sourceUnit, trackable, targetUnit));
		}

		private boolean tryUpdateTrackables(Trackable trackable) {
			if (trackable instanceof PushOnStack) {
				ImmutableIdentityHashSet<Trackable> set = ImmutableIdentityHashSet.<Trackable> empty();
				trackables = trackables.cons(set);
			} else if (trackable instanceof PopFromStack) {
				trackables = trackables.tail();
			} else {
				ImmutableIdentityHashSet<Trackable> set = trackables.head();
				if (set.contains(trackable))
					return false;
				trackables = trackables.tail().cons(set.add(trackable));
			}
			return true;
		}

		private boolean tryUpdateCallStack(Trackable trackable) {
			if (trackable instanceof PushOnStack) {
				callStack = callStack.cons(((PushOnStack) trackable).getCallSite());
			}

			if (trackable instanceof PopFromStack) {
				Unit callSite = ((PopFromStack) trackable).getCallSite();
				if (callStack.isNotEmpty() && callStack.head().equals(callSite)) {
					callStack = callStack.tail();
				} else {
					return false;
				}
			}

			return true;
		}

		private boolean tryUpdateCalledHierarchyMethods(Trackable trackable) {
			if (trackable instanceof PushOnStack) {
				Set<SootMethod> methods = getInitialDeclaration(((PushOnStack) trackable).getCallSite());
				ImmutableHashSet<SootMethod> newCalledHierarchyMethods = calledHierarchyMethods;
				for (SootMethod m : methods) {
					if (calledHierarchyMethods.contains(m))
						return false;
					else
						newCalledHierarchyMethods = newCalledHierarchyMethods.add(m);
				}
				calledHierarchyMethods = newCalledHierarchyMethods;
			} else if (trackable instanceof PopFromStack) {
				Set<SootMethod> methods = getInitialDeclaration(((PopFromStack) trackable).getCallSite());
				for (SootMethod m : methods) {
					calledHierarchyMethods = calledHierarchyMethods.remove(m);
				}
			}
			return true;
		}
	}
}
