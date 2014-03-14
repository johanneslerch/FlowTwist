package flow.twist.states;

import static flow.twist.trackable.Dummy.DUMMY;

import java.util.Collection;
import java.util.Set;

import soot.SootMethod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fj.F;
import fj.data.List;
import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.states.StateCache.ContextStateCache;
import flow.twist.states.StateCache.StateSinkNode;
import flow.twist.states.StateCache.StateStartNode;
import flow.twist.util.ImmutableLinkedHashSet;

public class AutomataMatcher {

	private Set<Path> paths = Sets.newHashSet();

	public AutomataMatcher(StateCache globalStateCache) {
		Multimap<SootMethod, StateStartNode> startStates = HashMultimap.create();

		for (ContextStateCache stateCache : globalStateCache.getAll()) {
			for (StateStartNode startState : stateCache.getAllStartStates()) {
				startStates.put(startState.method, startState);
			}
		}

		for (SootMethod startMethod : startStates.keySet()) {
			match(startStates.get(startMethod));
		}
	}

	private void match(Collection<StateStartNode> states) {
		Set<StateStartNode> forwards = Sets.newHashSet();
		Set<StateStartNode> backwards = Sets.newHashSet();

		for (StateStartNode state : states) {
			if (state.context.direction == AnalysisDirection.BACKWARDS)
				backwards.add(state);
			else
				forwards.add(state);
		}

		for (StateStartNode startForwards : forwards) {
			for (StateStartNode startBackwards : backwards) {
				match(startForwards, startBackwards, ImmutableLinkedHashSet.<TransitionTuple> empty());
			}
		}
	}

	private void match(StateNode startForwards, StateNode startBackwards, ImmutableLinkedHashSet<TransitionTuple> transitions) {
		if (startForwards instanceof StateSinkNode && startBackwards instanceof StateSinkNode) {
			createPath(transitions);
		}

		for (StateTransition transForwards : startForwards.getOutgoing()) {
			for (StateTransition transBackwards : startBackwards.getOutgoing()) {
				if (transForwards.condition == transBackwards.condition && hasSameCallSite(transForwards, transBackwards)
						&& (hasProgress(transForwards) || hasProgress(transBackwards))) {
					TransitionTuple tuple = new TransitionTuple(transForwards, transBackwards);
					if (!transitions.contains(tuple)) {
						match(transForwards.getTarget(), transBackwards.getTarget(), transitions.add(tuple));
					}
				}
			}
		}
	}

	private boolean hasProgress(StateTransition trans) {
		return trans.getSource() != trans.getTarget();
	}

	protected boolean hasSameCallSite(StateTransition transForwards, StateTransition transBackwards) {
		return transForwards.getPath().head().from == transBackwards.getPath().head().from;
	}

	private void createPath(ImmutableLinkedHashSet<TransitionTuple> transitions) {
		List<PathElement> forwards = List.nil();
		List<PathElement> backwards = List.nil();
		List<Object> stack = List.nil();

		for (TransitionTuple tuple : transitions) {
			forwards = appendPath(forwards, tuple.forwards);
			backwards = appendPath(backwards, tuple.backwards);
			stack = stack.cons(tuple.forwards.condition);
		}

		AnalysisContext context = transitions.head().forwards.getSource().context;
		paths.add(new Path(context, reverse(backwards).append(forwards), stack, null/* FIXME */));
	}

	public static List<PathElement> appendPath(List<PathElement> existing, StateTransition t) {
		List<PathElement> completePath = t.getPath();
		if (existing.isNotEmpty()) {
			if (t.getPath().isEmpty()) {
				PathElement connector = new PathElement(existing.last().to, DUMMY, t.connectingUnit);
				completePath = completePath.cons(connector);
			} else {
				PathElement connector = new PathElement(existing.last().to, DUMMY, t.getPath().head().from);
				completePath = completePath.cons(connector);
			}
		}
		completePath = existing.append(completePath);
		return completePath;
	}

	private List<PathElement> reverse(List<PathElement> path) {
		return path.reverse().map(new F<PathElement, PathElement>() {
			@Override
			public PathElement f(PathElement element) {
				return new PathElement(element.to, element.trackable, element.from);
			}
		});
	}

	public Set<Path> getValidPaths() {
		return paths;
	}

	private static class TransitionTuple {

		private StateTransition forwards;
		private StateTransition backwards;

		public TransitionTuple(StateTransition forwards, StateTransition backwards) {
			this.forwards = forwards;
			this.backwards = backwards;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((backwards.condition == null) ? 0 : backwards.condition.hashCode());
			result = prime * result + ((forwards.condition == null) ? 0 : forwards.condition.hashCode());
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
			TransitionTuple other = (TransitionTuple) obj;
			if (backwards.condition == null) {
				if (other.backwards.condition != null)
					return false;
			} else if (!backwards.condition.equals(other.backwards.condition))
				return false;
			if (forwards.condition == null) {
				if (other.forwards.condition != null)
					return false;
			} else if (!forwards.condition.equals(other.forwards.condition))
				return false;
			return true;
		}
	}
}
