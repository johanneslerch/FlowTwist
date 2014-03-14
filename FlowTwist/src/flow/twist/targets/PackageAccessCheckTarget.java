package flow.twist.targets;

import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import fj.data.Option;
import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.path.Path;
import flow.twist.pathchecker.PathChecker;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public class PackageAccessCheckTarget extends AnalysisTarget {

	private boolean enabled;

	@Override
	public boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie) {
		SootMethodRef method = ie.getMethodRef();
		boolean matches = (method.declaringClass().getName().equals("sun.reflect.misc.ReflectUtil") || method.declaringClass().getName()
				.equals("java.lang.SecurityManager"))
				&& method.name().equals("checkPackageAccess");
		return matches && !(ie.getArg(0) instanceof Constant);
	}

	@Override
	public KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left) {
		return kill();
	}

	@Override
	public KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie) {
		if (!enabled)
			return kill();

		Value classNameArg = ie.getArg(0);
		if (classNameArg instanceof NullConstant)
			return KillGenInfo.kill();

		Taint classNameTaint = new Taint(callSite, taint, classNameArg, ie.getMethod().getParameterType(0));
		classNameTaint.addPayload("checkPackageAccess");
		return KillGenInfo.propagate(classNameTaint);
	}

	@Override
	public Option<PathChecker> getPathChecker(Path path) {
		return Option.none();
	}

	@Override
	public void enableIfDeferred() {
		enabled = true;
	}

}
