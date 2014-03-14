package flow.twist.debugger;

import java.util.Set;

import flow.twist.config.AnalysisContext;
import flow.twist.debugger.Debugger.DebuggerListener;
import flow.twist.ifds.FlowEdge;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Trackable;

public class ConsoleDebugger implements DebuggerListener {

	@Override
	public void propagate(AnalysisContext context, Trackable source, Set<Trackable> propagatedTaints, FlowEdge edge) {
		System.out.println("---Propagating---");
		System.out.println("\t" + edge);
		System.out.println("\tSource Taint: " + source);
		System.out.println("\tNew Taints:");
		for (Trackable trackable : propagatedTaints) {
			System.out.println("\t\t" + trackable);
		}
	}

	@Override
	public void kill(AnalysisContext context, Trackable source, Propagator propagator, FlowEdge edge) {
		System.out.println("---Kill---");
		System.out.println("\t" + edge);
		System.out.println("\tSource Taint: " + source);
		System.out.println("\tKilled by: " + propagator.getClass().getName());
	}

	@Override
	public boolean hasChanged() {
		return false;
	}

	@Override
	public Object[] getHeader() {
		return null;
	}

	@Override
	public Object[][] getData() {
		return null;
	}

	@Override
	public void debugCallback(Object[][] data, int row, int col) {

	}
}
