package flow.twist.targets;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import fj.data.Option;
import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.config.AnalysisConfiguration.Type;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.pathchecker.EmptyCallStackChecker;
import flow.twist.pathchecker.PathChecker;
import flow.twist.pathchecker.StoreAnyPath;
import flow.twist.pathchecker.SubPathRemover;
import flow.twist.pathchecker.TwoPathChecker;
import flow.twist.trackable.Trackable;

public abstract class AnalysisTarget {

	protected PathChecker forwardsFromCallablePathChecker;
	protected PathChecker i2oPathChecker;

	protected AnalysisTarget() {
		i2oPathChecker = new TwoPathChecker(new SubPathRemover(new StoreAnyPath(this)));
		forwardsFromCallablePathChecker = new EmptyCallStackChecker(new StoreAnyPath(this));
	}

	public abstract boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie);

	public abstract KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left);

	public abstract KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie);

	public Option<PathChecker> getPathChecker(Path path) {
		if (path.context.type != Type.InnerToOuter) {
			for (PathElement element : path) {
				if (matches(path.context, element.from))
					return Option.some(forwardsFromCallablePathChecker);
			}
		} else if (matches(path.context, path.getFirst()))
			return Option.some(i2oPathChecker);
		return Option.none();
	}

	protected boolean matches(AnalysisContext context, Unit unit) {
		Stmt stmt = (Stmt) unit;
		if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			SootMethod enclosingMethod = context.icfg.getMethodOf(stmt);
			return matches(context.direction, enclosingMethod, ie);
		} else
			return false;
	}

	public abstract void enableIfDeferred();

}