package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator;
import flow.twist.targets.AnalysisTarget;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class I2OZeroHandler implements Propagator {

	private AnalysisContext context;

	public I2OZeroHandler(AnalysisContext context) {
		this.context = context;
	}

	@Override
	public boolean canHandle(Trackable taint) {
		return taint == Zero.ZERO;
	}

	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		return KillGenInfo.kill();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable taint, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable taint, Stmt callSite) {
		if (callSite instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) callSite;
			final InvokeExpr ie = assignStmt.getInvokeExpr();

			SootMethod enclosingMethod = context.icfg.getMethodOf(assignStmt);
			for (AnalysisTarget t : context.targets)
				if (t.matches(AnalysisDirection.FORWARDS, enclosingMethod, ie))
					return t.processForwardCallToReturn(taint, callSite, ie, getBase(assignStmt.getLeftOp()));

		}
		return kill();
	}

	private Value getBase(Value val) {
		if (val instanceof ArrayRef) {
			ArrayRef arrayRef = (ArrayRef) val;
			return arrayRef.getBase();
		}
		return val;
	}

}
