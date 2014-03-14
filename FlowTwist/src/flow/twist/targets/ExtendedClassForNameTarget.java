package flow.twist.targets;

import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;

public class ExtendedClassForNameTarget extends AnalysisTarget {

	@Override
	public boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie) {
		return ie.getMethodRef().name().equals("forName") && ie.getMethodRef().declaringClass().getName().equals("java.lang.Class")
				&& ie.getMethodRef().parameterTypes().size() == 3;
	}

	@Override
	public KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left) {
		Taint classInstance = new Taint(callSite, taint, left, ie.getMethod().getReturnType());
		return KillGenInfo.propagate(classInstance);
	}

	@Override
	public KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie) {
		Value classNameArg = ie.getArg(0);
		Value classLoaderArg = ie.getArg(2);

		if (classNameArg instanceof NullConstant || classLoaderArg instanceof NullConstant)
			return KillGenInfo.kill();
		SootMethod method = ie.getMethod();
		Taint classNameParameter = new Taint(callSite, taint, classNameArg, method.getParameterType(0));
		Taint classLoaderParameter = new Taint(callSite, taint, classLoaderArg, method.getParameterType(2));
		return KillGenInfo.propagate(classNameParameter, classLoaderParameter);
	}

	@Override
	public void enableIfDeferred() {
	}

}
