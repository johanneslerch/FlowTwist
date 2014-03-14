package flow.twist.debugger;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import flow.twist.config.AnalysisContext;
import flow.twist.ifds.FlowEdge;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Trackable;

public class Debugger {

	public enum EdgeType {
		NORMAL, CALL, CALL2RETURN, RETURN
	}

	public static interface DebuggerListener {
		public void kill(AnalysisContext context, Trackable source, Propagator propagator, FlowEdge edge);

		public void propagate(AnalysisContext context, Trackable source, Set<Trackable> propagatedTaints, FlowEdge edge);

		public Object[] getHeader();

		public Object[][] getData();

		public void debugCallback(Object[][] data, int row, int col);

		public boolean hasChanged();
	}

	public static interface Filter {

		/**
		 * @return false if edge should be ignored.
		 */
		public boolean filter(AnalysisContext context, FlowEdge edge);
	}

	private List<DebuggerListener> listeners = Lists.newLinkedList();
	private boolean paused = false;
	private List<Filter> filters = Lists.newLinkedList();

	public Debugger() {
	}

	public void registerListener(DebuggerListener listener) {
		listeners.add(listener);
	}

	public void kill(AnalysisContext context, Trackable source, Propagator propagator, FlowEdge edge) {
		checkPause();
		for (Filter filter : filters) {
			if (!filter.filter(context, edge))
				return;
		}
		for (DebuggerListener listener : listeners) {
			listener.kill(context, source, propagator, edge);
		}
	}

	public void propagate(AnalysisContext context, Trackable source, Set<Trackable> propagatedTaints, FlowEdge edge) {
		checkPause();
		for (Filter filter : filters) {
			if (!filter.filter(context, edge))
				return;
		}
		for (DebuggerListener listener : listeners) {
			listener.propagate(context, source, propagatedTaints, edge);
		}
	}

	private synchronized void checkPause() {
		while (paused) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void pause() {
		paused = true;
	}

	public synchronized void play() {
		paused = false;
		notifyAll();
	}

	public void addFilter(Filter filter) {
		this.filters.add(filter);
	}
}
