package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IdentityStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import flow.twist.TransitiveSinkCaller;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.reporter.Report;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class ArgumentSourceHandler implements Propagator {

	private final AnalysisContext context;
	private TransitiveSinkCaller transitiveSinkCaller;

	public ArgumentSourceHandler(AnalysisContext context, TransitiveSinkCaller transitiveSinkCaller) {
		this.context = context;
		this.transitiveSinkCaller = transitiveSinkCaller;
	}

	@Override
	public boolean canHandle(Trackable taint) {
		return taint instanceof Taint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable taint, Unit curr, Unit succ) {
		if (!(curr instanceof IdentityStmt))
			return identity();
		IdentityStmt idStmt = (IdentityStmt) curr;
		Value left = AnalysisUtil.getBackwardsBase(idStmt.getLeftOp());

		Taint t = (Taint) taint;

		if (!AnalysisUtil.maybeSameLocation(t.value, left))
			return identity();

		if (idStmt.getRightOp() instanceof ParameterRef) {
			if (t.value instanceof Local) {
				SootMethod m = context.icfg.getMethodOf(idStmt);
				if (AnalysisUtil.methodMayBeCallableFromApplication(m) && transitiveSinkCaller.isTransitiveCallerOfAnySink(m)) {
					context.reporter.reportTrackable(new Report(context, taint, curr));
				}
			}
		}
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable taint, Unit callStmt, SootMethod destinationMethod) {
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable taint, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable taint, Stmt callSite) {
		return identity();
	}
}
