package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.gen;
import static flow.twist.ifds.Propagator.KillGenInfo.identity;

import java.util.Set;

import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class ClassInstantiationPropagator implements Propagator {

	private Set<String> classMethodNames = Sets.newHashSet("newInstance", "getConstructor", "getConstructors", "getDeclaredConstructor",
			"getDeclaredConstructors", "getEnclosingConstructor", "getEnclosingClass", "getDeclaringClass");

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
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (!(callSite instanceof AssignStmt))
			return identity();

		AssignStmt assignStmt = (AssignStmt) callSite;
		if (!(assignStmt.getInvokeExpr() instanceof InstanceInvokeExpr))
			return identity();

		InstanceInvokeExpr ie = (InstanceInvokeExpr) assignStmt.getInvokeExpr();
		Taint t = (Taint) trackable;

		Value right = AnalysisUtil.getForwardsBase(ie.getBase());
		if (!AnalysisUtil.maybeSameLocation(t.value, right))
			return identity();

		Value left = AnalysisUtil.getForwardsBase(assignStmt.getLeftOp());
		SootMethodRef method = callSite.getInvokeExpr().getMethodRef();
		String methodName = method.name();
		String className = method.declaringClass().getName();

		if ((className.equals("java.lang.Class") && classMethodNames.contains(methodName))
				|| (className.equals("java.lang.reflect.Constructor") && methodName.equals("newInstance"))) {
			return gen(t.createAlias(left, callSite));
		}
		return identity();
	}

}
