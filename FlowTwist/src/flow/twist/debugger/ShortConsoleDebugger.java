package flow.twist.debugger;

import java.util.Set;

import flow.twist.config.AnalysisContext;
import flow.twist.debugger.Debugger.DebuggerListener;
import flow.twist.ifds.FlowEdge;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Trackable;

public class ShortConsoleDebugger implements DebuggerListener {

	@Override
	public void propagate(AnalysisContext context, Trackable source, Set<Trackable> propagatedTaints, FlowEdge edge) {

		for (Trackable trackable : propagatedTaints) {
			if (source != trackable) {
				System.out.println(System.identityHashCode(source) + "\t\t" + source + "\tpropagated over " + edge);
				System.out.println("->\t" + System.identityHashCode(trackable) + "\t" + trackable);
			}
		}
	}

	@Override
	public void kill(AnalysisContext context, Trackable source, Propagator propagator, FlowEdge edge) {
		// System.out.println(System.identityHashCode(source) + " " + source +
		// "\tkilled");
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
