package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.gen;
import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.ReturnEdgeTaint;
import flow.twist.trackable.ReturnValueTaint;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;

public class DefaultTaintPropagator implements Propagator {

	private AnalysisContext context;

	public DefaultTaintPropagator(AnalysisContext context) {
		this.context = context;
	}

	@Override
	public boolean canHandle(Trackable taint) {
		return taint instanceof Taint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		if (!(curr instanceof DefinitionStmt))
			return identity();

		Taint taint = (Taint) trackable;
		DefinitionStmt defStmt = (DefinitionStmt) curr;
		// extract "base" locals of array refs
		Value leftBase = AnalysisUtil.getBackwardsBase(defStmt.getLeftOp());
		Value rightBase = AnalysisUtil.getBackwardsBase(defStmt.getRightOp());

		if (!AnalysisUtil.maybeSameLocation(taint.value, leftBase))
			return identity();
		if (leftBase.equals(rightBase)) // x = x; or x[i] = x[j]; -> do nothing
			return identity();

		if (defStmt.getLeftOp() instanceof Local) { // Local
			if (defStmt.getRightOp() instanceof Constant || !AnalysisUtil.isAssignable(taint.type, rightBase.getType()))
				return kill();

			return propagate(taint.createAlias(rightBase, curr));
		} else { // Field
			if (defStmt.getRightOp() instanceof Constant || !AnalysisUtil.isAssignable(taint.type, rightBase.getType()))
				return identity();

			// if the stmt defines a local then we know that this value we
			// definitely overwritten;
			// if it does not define a local (but, say, a FieldRef) then the
			// value *may* have been
			// overwritten; hence we need to track the source, too
			return gen(taint.createAlias(rightBase, curr));
		}
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		Taint taint = (Taint) trackable;

		if (callStmt instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) callStmt;
			if (taint.value.equals(assignStmt.getLeftOp())) {
				if (AnalysisUtil.isAssignable(taint.type, destinationMethod.getReturnType())) {
					for (Unit u : context.icfg.getStartPointsOf(destinationMethod)) {
						if (u instanceof ReturnStmt) {
							ReturnStmt returnStmt = (ReturnStmt) u;
							Value retValue = returnStmt.getOp();

							if (retValue instanceof Constant)
								continue;

							// There is at least one non constant return stmt
							return propagate(new ReturnValueTaint(callStmt, trackable, taint.type));
						}
					}
				}
			}
		}

		return kill();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable taint, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (callSite == null)
			return kill();

		Taint t = (Taint) taint;
		Stmt s = (Stmt) callSite;
		final int paramCount = calleeMethod.getParameterCount();
		final Value[] actualParams = new Value[paramCount];
		for (int i = 0; i < paramCount; i++) {
			actualParams[i] = s.getInvokeExpr().getArg(i);
		}

		for (int i = 0; i < paramCount; i++) {
			Type parameterType = calleeMethod.getParameterType(i);
			Value parameterRef = Jimple.v().newParameterRef(parameterType, i);
			if (parameterRef.equivTo(t.value)) {
				Value actualParam = actualParams[i];
				// don't track constants
				if (actualParam instanceof Local && AnalysisUtil.isAssignable(t.type, actualParam.getType())) {
					return propagate(new ReturnEdgeTaint(callSite, exitStmt, taint, actualParam, i, t.type));
				}
			}
		}
		return kill();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (!(callSite instanceof DefinitionStmt))
			return identity();

		Taint taint = (Taint) trackable;
		DefinitionStmt defStmt = (DefinitionStmt) callSite;
		if (!defStmt.getLeftOp().equals(taint.value))
			return identity();

		if (defStmt.getLeftOp() instanceof Local) {
			return kill();
		} else {
			// if the stmt defines a local then we know that this value we
			// definitely overwritten;
			// if it does not define a local (but, say, a FieldRef) then the
			// value *may* have been
			// overwritten; hence we need to track the source, too
			return identity();
		}
	}

}
