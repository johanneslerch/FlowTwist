package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.util.AnalysisUtil.isTarget;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisConfiguration.Type;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public class PropagateOverTarget implements Propagator {

	private AnalysisContext context;

	public PropagateOverTarget(AnalysisContext context) {
		this.context = context;
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
		if (isTarget(context, callStmt)) {
			return kill();
		}
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt,
			Unit returnSite) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (isTarget(context, callSite)) {
			Taint taint = (Taint) trackable;
			InvokeExpr ie = callSite.getInvokeExpr();
			boolean argsTainted = false;
			boolean receiverIsTainted = false;
			if (ie instanceof InstanceInvokeExpr) {
				Value receiver = ((InstanceInvokeExpr) ie).getBase();
				if (receiver.equals(taint.value)) {
					receiverIsTainted = true;
				}
			}

			for (Value arg : ie.getArgs()) {
				if (arg.equivTo(taint.value)) {
					argsTainted = true;
					break;
				}
			}

			if (argsTainted || receiverIsTainted) {
				Set<Taint> newTaints = Sets.newHashSet();
				if (callSite instanceof AssignStmt) {
					Value left = ((AssignStmt) callSite).getLeftOp();
					Taint returnValueTaint = new Taint(callSite, taint, left, left.getType());
					if (context.type != Type.InnerToOuter)
						returnValueTaint.addPayload("passedSink");
					newTaints.add(returnValueTaint);
				}
				if (!receiverIsTainted && ie instanceof InstanceInvokeExpr) {
					Value receiver = ((InstanceInvokeExpr) ie).getBase();
					newTaints.add(new Taint(callSite, taint, receiver, receiver.getType()));
				}
				return new KillGenInfo(false, newTaints);
			}
		}
		return identity();
	}
}
