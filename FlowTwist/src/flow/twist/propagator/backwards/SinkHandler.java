package flow.twist.propagator.backwards;

import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator;
import flow.twist.targets.AnalysisTarget;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class SinkHandler implements Propagator {

	private AnalysisContext context;

	public SinkHandler(AnalysisContext context) {
		this.context = context;
	}

	@Override
	public boolean canHandle(Trackable taint) {
		return taint == Zero.ZERO;
	}

	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		return KillGenInfo.kill();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable taint, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable taint, Stmt callSite) {
		final InvokeExpr ie = callSite.getInvokeExpr();

		SootMethod enclosingMethod = context.icfg.getMethodOf(callSite);
		for (AnalysisTarget t : context.targets) {
			if (t.matches(AnalysisDirection.BACKWARDS, enclosingMethod, ie))
				return t.processBackwardCallToReturn(taint, callSite, ie);
		}

		throw new IllegalStateException();
	}

}
