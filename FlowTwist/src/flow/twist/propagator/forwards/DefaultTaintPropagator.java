package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.gen;
import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;

import java.util.ArrayList;
import java.util.Iterator;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import com.google.common.collect.Lists;

import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.ParameterTaint;
import flow.twist.trackable.ReturnEdgeTaint;
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
		Taint taint = (Taint) trackable;
		if (curr instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) curr;

			Value rightBase = AnalysisUtil.getForwardsBase(defStmt.getRightOp());
			Value leftBase = AnalysisUtil.getForwardsBase(defStmt.getLeftOp());

			if (leftBase.equals(rightBase)) // x = x; or x[i] = x[j]; -> do
											// nothing
				return identity();

			// kill the LHS if it is assigned another value
			Value left = defStmt.getLeftOp();
			if (left instanceof Local && taint.value.equals(left)) {
				return kill();
			}

			if (rightBase instanceof CastExpr) {
				CastExpr castExpr = (CastExpr) rightBase;
				if (AnalysisUtil.maybeSameLocation(taint.value, castExpr.getOp())) {
					if (AnalysisUtil.isAssignable(taint.type, leftBase.getType())) {
						return gen(taint.createAlias(leftBase, curr));
					} else {
						return identity();
					}
				}
			}

			// at assignments, propagate taint from right to left
			if (taint.value.equivTo(rightBase) || AnalysisUtil.maybeSameLocation(taint.value, rightBase)) {
				return gen(taint.createAlias(leftBase, curr));
			}
		}
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		if (!destinationMethod.hasActiveBody())
			return kill();

		Taint taint = (Taint) trackable;
		Stmt s = (Stmt) callStmt;
		ArrayList<Taint> propagations = Lists.newArrayList();

		final int paramCount = destinationMethod.getParameterCount();
		Value[] destinationValues = getParameterValues(destinationMethod);
		for (int i = 0; i < paramCount; i++) {
			if (s.getInvokeExpr().getArg(i).equivTo(taint.value)) {
				propagations.add(new ParameterTaint(callStmt, taint, destinationValues[i], taint.type));
			}
		}

		if (propagations.size() == 0)
			return kill();
		else
			return propagate(propagations.toArray(new Trackable[propagations.size()]));
	}

	private Value[] getParameterValues(SootMethod destinationMethod) {
		int paramsFound = 0;
		Value[] result = new Value[destinationMethod.getParameterCount()];
		for (Unit unit : destinationMethod.getActiveBody().getUnits()) {
			if (!(unit instanceof IdentityStmt))
				continue;

			Value rightOp = ((IdentityStmt) unit).getRightOp();
			if (!(rightOp instanceof ParameterRef))
				continue;

			ParameterRef paramRef = (ParameterRef) rightOp;
			result[paramRef.getIndex()] = paramRef;
			paramsFound++;

			if (paramsFound == result.length)
				break;
		}
		return result;
	}

	public Value getParameter(SootMethod destinationMethod, int i) {
		Iterator<Unit> unitsIt = destinationMethod.getActiveBody().getUnits().iterator();
		while (unitsIt.hasNext()) {
			Unit s = unitsIt.next();
			if (s instanceof IdentityStmt) {
				Value rightOp = ((IdentityStmt) s).getRightOp();
				if (rightOp instanceof ParameterRef && ((ParameterRef) rightOp).getIndex() == i) {
					return rightOp;
				}
			}
		}

		throw new RuntimeException("couldn't find parameterref for index " + i + " in " + destinationMethod);
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		Taint taint = (Taint) trackable;

		if (!(exitStmt instanceof ReturnStmt))
			return kill();

		if (!(callSite instanceof AssignStmt))
			return kill();

		ReturnStmt returnStmt = (ReturnStmt) exitStmt;
		AssignStmt assignStmt = (AssignStmt) callSite;

		if (!returnStmt.getOp().equivTo(taint.value))
			return kill();

		Value left = AnalysisUtil.getForwardsBase(assignStmt.getLeftOp());
		// return propagate(taint.createAlias(left, callSite));
		// return propagate(taint.createAlias(left, exitStmt));
		return propagate(new ReturnEdgeTaint(callSite, exitStmt, taint, left, -1, taint.type));
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		Taint taint = (Taint) trackable;
		if (callSite instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) callSite;
			Value left = assignStmt.getLeftOp();

			if (left instanceof Local && taint.value.equals(left)) {
				return kill();
			}
		}

		return identity();
	}

}
