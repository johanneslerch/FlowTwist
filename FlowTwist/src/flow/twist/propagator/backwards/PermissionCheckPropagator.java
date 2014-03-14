package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class PermissionCheckPropagator implements Propagator {

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
		Taint taint = (Taint) trackable;
		InvokeExpr ie = ((Stmt) callSite).getInvokeExpr();
		SootMethodRef ref = ie.getMethodRef();
		if (ref.declaringClass().getName().equals("sun.reflect.misc.ReflectUtil")
				|| ref.declaringClass().getName().equals("java.lang.SecurityManager") && ref.name().equals("checkPackageAccess")
				&& ie.getArg(0).equals(taint.value))
			return kill();

		return identity();
	}
}
