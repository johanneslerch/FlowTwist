package flow.twist.targets;

import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

/*
 * Might be interesting to catch things as in Security Explorations Issue 61 - CVE-2013-2460
 */
public class MethodInvokeTarget extends AnalysisTarget {

	@Override
	public boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie) {
		boolean matches = ie.getMethodRef().name().equals("invoke")
				&& ie.getMethodRef().declaringClass().getName().equals("java.lang.reflect.Method");

		return matches;
	}

	@Override
	public KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left) {
		// TODO: taint invocation result if there is one?
		return KillGenInfo.kill();
	}

	@Override
	public KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie) {

		Value methodObject = ((InstanceInvokeExpr) ie).getBase();

		Taint methodObjectTaint = new Taint(callSite, taint, methodObject, methodObject.getType());
		return KillGenInfo.propagate(methodObjectTaint);
	}

	@Override
	public void enableIfDeferred() {
	}

}
