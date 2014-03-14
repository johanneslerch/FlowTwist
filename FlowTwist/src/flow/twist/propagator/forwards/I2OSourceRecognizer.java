package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import flow.twist.TransitiveSinkCaller;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.reporter.Report;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class I2OSourceRecognizer implements Propagator {

	private final AnalysisContext context;
	private TransitiveSinkCaller transitiveSinkCaller;

	public I2OSourceRecognizer(AnalysisContext context, TransitiveSinkCaller transitiveSinkCaller) {
		this.context = context;
		this.transitiveSinkCaller = transitiveSinkCaller;
	}

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof Taint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (!(exitStmt instanceof ReturnStmt))
			return identity();

		Taint taint = (Taint) trackable;

		ReturnStmt returnStmt = (ReturnStmt) exitStmt;
		Value retVal = AnalysisUtil.getForwardsBase(returnStmt.getOp());
		if (AnalysisUtil.maybeSameLocation(taint.value, retVal)) {
			SootMethod m = context.icfg.getMethodOf(returnStmt);
			if (AnalysisUtil.methodMayBeCallableFromApplication(m) && transitiveSinkCaller.isTransitiveCallerOfAnySink(m)) {
				context.reporter.reportTrackable(new Report(context, taint, exitStmt));
			}
		}
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		return identity();
	}

}
