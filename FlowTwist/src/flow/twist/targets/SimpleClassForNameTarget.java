package flow.twist.targets;

import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;

public class SimpleClassForNameTarget extends AnalysisTarget {

	/*
	 * (non-Javadoc)
	 * 
	 * @see targets.AnalysisTarget#matches(soot.jimple.InvokeExpr)
	 */
	@Override
	public boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie) {
		boolean matches = ie.getMethodRef().name().equals("forName") && ie.getMethodRef().declaringClass().getName().equals("java.lang.Class")
				&& ie.getMethodRef().parameterTypes().size() == 1;

		if (matches && !(ie.getArg(0) instanceof Constant)) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * targets.AnalysisTarget#processForwardCallToReturn(permissionAnalysis.
	 * trackables.Trackable, soot.jimple.Stmt, soot.jimple.InvokeExpr,
	 * soot.Value)
	 */
	@Override
	public KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left) {
		Taint classInstance = new Taint(callSite, taint, left, ie.getMethod().getReturnType());
		return KillGenInfo.propagate(classInstance);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * targets.AnalysisTarget#processBackwardCallToReturn(permissionAnalysis
	 * .trackables.Trackable, soot.jimple.Stmt, soot.jimple.InvokeExpr)
	 */
	@Override
	public KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie) {
		Value classNameArg = ie.getArg(0);
		if (classNameArg instanceof NullConstant)
			return KillGenInfo.kill();

		Taint classNameParameter = new Taint(callSite, taint, classNameArg, ie.getMethod().getParameterType(0));
		return KillGenInfo.propagate(classNameParameter);
	}

	@Override
	public void enableIfDeferred() {

	}
}
