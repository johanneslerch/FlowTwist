package flow.twist.propagator.forwards;

import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisConfiguration.Type;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;

public class ZeroAtParameterHandler implements Propagator {

	private AnalysisContext context;

	public ZeroAtParameterHandler(AnalysisContext context) {
		this.context = context;
	}

	@Override
	public boolean canHandle(Trackable taint) {
		return taint == Zero.ZERO;
	}

	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		if (curr instanceof IdentityStmt) {
			IdentityStmt idStmt = (IdentityStmt) curr;
			if (idStmt.getRightOp().toString().contains("@parameter")) {
				if (context.type == Type.ForwardsFromAllParameters || idStmt.getRightOp().getType().toString().equals("java.lang.String"))
					return KillGenInfo.propagate(new Taint(curr, trackable, idStmt.getLeftOp(), idStmt.getLeftOp().getType()));
			}
		}

		return KillGenInfo.kill();
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
		throw new IllegalArgumentException();
	}
}
