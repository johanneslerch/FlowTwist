package flow.twist.debugger;

import java.util.Set;

import soot.SootMethod;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.debugger.Debugger.DebuggerListener;
import flow.twist.ifds.FlowEdge;
import flow.twist.ifds.Propagator;
import flow.twist.ifds.FlowEdge.Call2ReturnEdge;
import flow.twist.ifds.FlowEdge.CallEdge;
import flow.twist.ifds.FlowEdge.NormalEdge;
import flow.twist.ifds.FlowEdge.ReturnEdge;
import flow.twist.reporter.DebugPlotter;
import flow.twist.reporter.Report;
import flow.twist.trackable.Trackable;
import flow.twist.util.CacheMap;

public class FanInFanOutDebugger implements DebuggerListener {

	private CacheMap<SootMethod, Set<Trackable>> fanIn = new CacheMap<SootMethod, Set<Trackable>>() {
		@Override
		protected Set<Trackable> createItem(SootMethod key) {
			return Sets.newIdentityHashSet();
		}
	};
	private CacheMap<SootMethod, Set<Trackable>> fanOut = new CacheMap<SootMethod, Set<Trackable>>() {
		@Override
		protected Set<Trackable> createItem(SootMethod key) {
			return Sets.newIdentityHashSet();
		}
	};
	private CacheMap<SootMethod, Set<Trackable>> intra = new CacheMap<SootMethod, Set<Trackable>>() {
		@Override
		protected Set<Trackable> createItem(SootMethod key) {
			return Sets.newIdentityHashSet();
		}
	};
	private AnalysisContext context;
	private boolean dirty = true;

	@Override
	public synchronized void kill(AnalysisContext context, Trackable source, Propagator propagator, FlowEdge edge) {
	}

	@Override
	public synchronized void propagate(final AnalysisContext context, final Trackable source, Set<Trackable> propagatedTaints, FlowEdge edge) {
		this.context = context;
		edge.accept(new FlowEdge.Visitor<Void>() {
			@Override
			public Void visit(Call2ReturnEdge call2ReturnEdge) {
				intra.getOrCreate(context.icfg.getMethodOf(call2ReturnEdge.callSite)).add(source);
				dirty = true;
				return null;
			}

			@Override
			public Void visit(ReturnEdge returnEdge) {
				if (returnEdge.returnSite == null)
					return null;

				SootMethod method = context.icfg.getMethodOf(returnEdge.returnSite);
				fanOut.getOrCreate(method).add(source);
				dirty = true;
				return null;
			}

			@Override
			public Void visit(CallEdge callEdge) {
				fanIn.getOrCreate(callEdge.destinationMethod).add(source);
				dirty = true;
				return null;
			}

			@Override
			public Void visit(NormalEdge normalEdge) {
				intra.getOrCreate(context.icfg.getMethodOf(normalEdge.curr)).add(source);
				dirty = true;
				return null;
			}
		});
	}

	@Override
	public String[] getHeader() {
		return new String[] { "Method", "FanIn", "FanOut", "Intra" };
	}

	@Override
	public synchronized Object[][] getData() {
		Set<SootMethod> keys = Sets.newHashSet();
		keys.addAll(fanIn.keySet());
		keys.addAll(fanOut.keySet());
		keys.addAll(intra.keySet());

		Object[][] result = new Object[keys.size()][4];
		int i = 0;
		for (SootMethod key : keys) {
			result[i][0] = key;
			result[i][1] = fanIn.getOrCreate(key).size();
			result[i][2] = fanOut.getOrCreate(key).size();
			result[i][3] = intra.getOrCreate(key).size();
			i++;
		}

		dirty = false;
		return result;
	}

	@Override
	public synchronized void debugCallback(Object[][] data, int row, int col) {
		SootMethod key = (SootMethod) data[row][0];
		String methodName = (key.getDeclaringClass() + "." + key.getName()).replaceAll("[^\\w\\.]", "_");
		plot(fanIn.getOrCreate(key), "debug/" + methodName + "_fanIn");
		plot(fanOut.getOrCreate(key), "debug/" + methodName + "_fanOut");
		plot(intra.getOrCreate(key), "debug/" + methodName + "_intra");
	}

	private void plot(Set<Trackable> data, String filename) {
		DebugPlotter plotter = new DebugPlotter();
		for (Trackable t : data) {
			if (t.sourceUnit == null)
				continue;
			plotter.reportTrackable(new Report(context, t, t.sourceUnit));
		}
		plotter.writeFile(filename);
	}

	@Override
	public synchronized boolean hasChanged() {
		return dirty;
	}
}
