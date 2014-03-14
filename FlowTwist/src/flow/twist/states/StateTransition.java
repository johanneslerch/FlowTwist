package flow.twist.states;

import soot.SootMethod;
import soot.Unit;
import fj.data.List;
import flow.twist.path.PathElement;

public class StateTransition {

	// FIXME: Declaring class is missing
	public final SootMethod condition;
	private final boolean isPushOnStack;
	private final List<PathElement> path;
	private StateNode source;
	private StateNode target;
	public final Unit connectingUnit;

	public StateTransition(SootMethod condition, boolean isPushOnStack, List<PathElement> path, Unit connectingUnit) {
		super();
		this.condition = condition;
		this.isPushOnStack = isPushOnStack;
		this.path = path;
		this.connectingUnit = connectingUnit;
	}

	public StateNode getSource() {
		return source;
	}

	public void setSource(StateNode source) {
		this.source = source;
	}

	public StateNode getTarget() {
		return target;
	}

	public void setTarget(StateNode target) {
		this.target = target;
	}

	public boolean isPushOnStack() {
		return isPushOnStack;
	}

	public List<PathElement> getPath() {
		return path;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (PathElement p : path) {
			builder.append(p.toString());
			builder.append("\n");
		}
		return builder.toString();
	}

}
