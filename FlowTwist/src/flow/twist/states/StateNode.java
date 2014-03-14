package flow.twist.states;

import java.util.LinkedList;
import java.util.List;

import flow.twist.config.AnalysisContext;

public abstract class StateNode {

	private final List<StateTransition> incoming = new LinkedList<StateTransition>();
	private final List<StateTransition> outgoing = new LinkedList<StateTransition>();
	protected AnalysisContext context;

	protected StateNode(AnalysisContext context) {
		this.context = context;
	}

	public void addIncomingTransition(StateTransition s) {
		incoming.add(s);
		s.setTarget(this);
	}

	public void addOutgoingTransition(StateTransition t) {
		outgoing.add(t);
		t.setSource(this);
	}

	public void removeAllConnectionsRecursively() {
		for (StateTransition inc : incoming) {
			inc.getSource().outgoing.remove(inc);
			if (inc.getSource().outgoing.isEmpty())
				inc.getSource().removeAllConnectionsRecursively();
		}
		incoming.clear();
		for (StateTransition out : outgoing) {
			out.getTarget().incoming.remove(out);
			if (out.getTarget().incoming.isEmpty())
				out.getTarget().removeAllConnectionsRecursively();
		}
		outgoing.clear();
	}

	// private List<List<PathElement>> mergePaths(StateTransition first,
	// StateTransition second, StateTransition third) {
	// List<List<PathElement>> newPaths = new LinkedList<List<PathElement>>();
	//
	// for (List<PathElement> initialPath : first.getPath()) {
	// List<PathElement> firstPath = new LinkedList<PathElement>(initialPath);
	//
	// for (List<PathElement> callPath : second.getPath()) {
	// List<PathElement> secondPath = new LinkedList<PathElement>(firstPath);
	// secondPath.addAll(callPath);
	//
	// for (List<PathElement> returnPath : third.getPath()) {
	// List<PathElement> thirdPath = new LinkedList<PathElement>(secondPath);
	// thirdPath.addAll(returnPath);
	//
	// newPaths.add(thirdPath);
	// }
	// }
	// }
	// return newPaths;
	// }

	public List<StateTransition> getIncoming() {
		return incoming;
	}

	public List<StateTransition> getOutgoing() {
		return outgoing;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("State ..." + hashCode() + "\n\t");
		for (StateTransition transition : outgoing) {
			builder.append(transition.toString().replaceAll("\n", "\n\t"));
			builder.append("-> " + transition.getTarget().getClass());
		}
		return builder.toString();
	}

}
