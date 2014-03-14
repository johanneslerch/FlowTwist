package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;

import java.util.Set;

import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public abstract class MethodFilteringPropagator implements Propagator {

	protected final Set<String> methodSigs;

	public MethodFilteringPropagator(String... methodSigs) {
		this.methodSigs = Sets.newHashSet(methodSigs);
	}

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof Taint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable taint, Unit curr, Unit succ) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable taint, Unit callSite, SootMethod destinationMethod) {
		if (callSite instanceof AssignStmt) {
			final Stmt s = (Stmt) callSite;
			final InvokeExpr ie = s.getInvokeExpr();
			final SootMethodRef m = ie.getMethodRef();

			if (methodSigs.contains(m.getSignature())) {
				return kill();
			}
		}
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable taint, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		Taint taint = (Taint) trackable;
		final Stmt s = (Stmt) callSite;
		final InvokeExpr ie = s.getInvokeExpr();
		final SootMethodRef m = ie.getMethodRef();

		if (methodSigs.contains(m.getSignature())) {
			return handleMethodCall(taint, callSite, ie);
		}
		return identity();
	}

	protected abstract KillGenInfo handleMethodCall(Taint taint, Stmt callSite, InvokeExpr ie);

}
