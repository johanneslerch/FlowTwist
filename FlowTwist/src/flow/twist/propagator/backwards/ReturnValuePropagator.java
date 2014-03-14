package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.ReturnValueTaint;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class ReturnValuePropagator implements Propagator {

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof ReturnValueTaint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		if (curr instanceof ReturnStmt) {
			Value retValue = getBase(((ReturnStmt) curr).getOp());
			if (retValue instanceof Constant) {
				return kill();
			} else {
				ReturnValueTaint retValTaint = (ReturnValueTaint) trackable;
				if (AnalysisUtil.isAssignable(retValTaint.type, retValue.getType()))
					return propagate(new Taint(curr, trackable, retValue, retValTaint.type));
				else
					return kill();
			}
		} else if (curr instanceof ThrowStmt)
			return kill();

		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		throw new IllegalStateException();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		throw new IllegalStateException();
	}

	/**
	 * extracts array base values from array refs and values from cast
	 * expressions
	 */
	private Value getBase(Value val) {
		if (val instanceof ArrayRef) {
			ArrayRef arrayRef = (ArrayRef) val;
			return arrayRef.getBase();
		}
		if (val instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) val;
			return castExpr.getOp();
		}
		return val;
	}
}
