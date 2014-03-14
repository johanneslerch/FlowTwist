package flow.twist.propagator;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class JavaUtilKiller implements Propagator {

	private AnalysisContext context;

	public JavaUtilKiller(AnalysisContext context) {
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
		InvokeExpr ie = ((Stmt) callStmt).getInvokeExpr();
		if (isJavaUtilMethod(ie.getMethod()))
			return kill();
		else
			return identity();
	}

	private boolean isJavaUtilMethod(SootMethod method) {
		if (method == null)
			return false;

		for (SootMethod initMethodDecl : AnalysisUtil.getInitialDeclaration(method)) {
			SootClass clazz = initMethodDecl.getDeclaringClass();
			if (clazz.getPackageName().equals("java.util") || clazz.getPackageName().startsWith("java.util.concurrent")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (returnSite != null && isJavaUtilMethod(context.icfg.getMethodOf(returnSite)))
			return kill();
		else
			return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		return identity();
	}

}
