package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.util.AnalysisUtil.isTarget;

import java.util.Set;

import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

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
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (isTarget(context, callSite)) {
			Taint taint = (Taint) trackable;
			boolean returnValueTainted = false;
			boolean receiverTainted = false;

			if (callSite instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) callSite;
				if (taint.value.equals(assignStmt.getLeftOp())) {
					returnValueTainted = true;
				}
			}
			if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
				Value receiver = iie.getBase();
				if (taint.value.equals(receiver))
					receiverTainted = true;
			}

			if (returnValueTainted || receiverTainted) {
				InvokeExpr ie = callSite.getInvokeExpr();
				Set<Taint> taints = Sets.newHashSet();
				if (!receiverTainted && ie instanceof InstanceInvokeExpr) {
					Value receiver = ((InstanceInvokeExpr) ie).getBase();
					taints.add(new Taint(callSite, taint, receiver, receiver.getType()));
				}

				for (int i = 0; i < ie.getMethod().getParameterCount(); i++) {
					Type parameterType = ie.getMethod().getParameterType(i);
					if (!(parameterType instanceof PrimType) && !(ie.getArg(i) instanceof Constant)) {
						taints.add(new Taint(callSite, taint, ie.getArg(i), parameterType));
					}
				}

				return new KillGenInfo(returnValueTainted && !receiverTainted, taints);
			}
		}
		return identity();
	}

}
