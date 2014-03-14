package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;

import java.util.Collections;
import java.util.HashSet;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.reporter.Report;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class PayloadSourceRecognizer implements Propagator {

	private final AnalysisContext context;
	private HashSet<SootMethod> seedMethods;

	public PayloadSourceRecognizer(AnalysisContext context) {
		this.context = context;
		this.seedMethods = Sets.newHashSet();
		for (Unit seed : context.seedFactory.initialSeeds(context)) {
			seedMethods.add(context.icfg.getMethodOf(seed));
		}
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
		boolean isSeedMethod = seedMethods.contains(context.icfg.getMethodOf(exitStmt));

		if (!(exitStmt instanceof ReturnStmt))
			return new KillGenInfo(isSeedMethod, Collections.<Trackable> emptyList());

		Taint taint = (Taint) trackable;
		ReturnStmt returnStmt = (ReturnStmt) exitStmt;
		Value retVal = AnalysisUtil.getForwardsBase(returnStmt.getOp());
		if (isSeedMethod && AnalysisUtil.maybeSameLocation(taint.value, retVal)) {
			SootMethod m = context.icfg.getMethodOf(returnStmt);
			if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
				if (!trackable.getPayload().isEmpty()) {
					context.reporter.reportTrackable(new Report(context, taint, exitStmt));
				}
			}
		}
		return new KillGenInfo(isSeedMethod, Collections.<Trackable> emptyList());
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		return identity();
	}

}
