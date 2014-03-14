package flow.twist;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;

public class TransitiveSinkCaller {

	private Set<SootMethod> transitiveCallers = Sets.newHashSet();
	private AnalysisContext context;

	public TransitiveSinkCaller(AnalysisContext context) {
		this.context = context;
		for (Unit sink : context.seedFactory.initialSeeds(context)) {
			SootMethod m = context.icfg.getMethodOf(sink);
			addTransitiveCallers(m);
		}
	}

	private void addTransitiveCallers(SootMethod m) {
		if (transitiveCallers.add(m)) {
			for (Unit caller : context.icfg.getCallersOf(m)) {
				addTransitiveCallers(context.icfg.getMethodOf(caller));
			}
		}
	}

	public boolean isTransitiveCallerOfAnySink(SootMethod m) {
		return transitiveCallers.contains(m);
	}
}
