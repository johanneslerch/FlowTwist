package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import flow.twist.propagator.backwards.MethodFilteringPropagator;
import flow.twist.trackable.Taint;
import flow.twist.util.AnalysisUtil;

public class ShortcutPropagator extends MethodFilteringPropagator {

	private static final String VALUE_OF = "<java.lang.String: java.lang.String valueOf(java.lang.Object)>";

	public ShortcutPropagator() {
		super(VALUE_OF);
	}

	@Override
	protected KillGenInfo handleMethodCall(Taint taint, Stmt callSite, InvokeExpr ie) {
		if (ie.getMethodRef().getSignature().equals(VALUE_OF)) {
			if (callSite instanceof AssignStmt) {
				AssignStmt definitionStmt = (AssignStmt) callSite;
				Value argValue = definitionStmt.getInvokeExpr().getArg(0);
				if (AnalysisUtil.maybeSameLocation(taint.value, argValue)) {
					final Value returnValue = definitionStmt.getLeftOp();
					return propagate(new Taint(callSite, taint, returnValue, returnValue.getType()));
				}
			}
		}
		return identity();
	}
}
