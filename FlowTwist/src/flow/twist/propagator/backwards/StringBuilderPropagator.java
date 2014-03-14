package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;

import java.util.Set;

import soot.RefType;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.trackable.Taint;

//TODO handle StringBuilder.insert etc.
public class StringBuilderPropagator extends MethodFilteringPropagator {

	public StringBuilderPropagator() {
		super("<java.lang.StringBuffer: java.lang.String toString()>", "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>",
				"<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.CharSequence)>",
				"<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>",
				"<java.lang.StringBuffer: void <init>(java.lang.String)>", "<java.lang.StringBuffer: void <init>(java.lang.CharSequence)>",
				"<java.lang.StringBuffer: void <init>()>", "<java.lang.StringBuilder: java.lang.String toString()>",
				"<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>",
				"<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence)>",
				"<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>",
				"<java.lang.StringBuilder: void <init>(java.lang.String)>", "<java.lang.StringBuilder: void <init>(java.lang.CharSequence)>",
				"<java.lang.StringBuilder: void <init>()>");
	}

	@Override
	protected KillGenInfo handleMethodCall(Taint taint, Stmt callSite, InvokeExpr ie) {
		if (isStringBuilderToString(ie)) {
			if (callSite instanceof DefinitionStmt) {
				DefinitionStmt definitionStmt = (DefinitionStmt) callSite;
				final Value toStringReturnValue = definitionStmt.getLeftOp();
				if (taint.value.equals(toStringReturnValue)) {
					InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
					Value stringBuilderObject = iie.getBase();
					return propagate(new Taint(callSite, taint, stringBuilderObject, stringBuilderObject.getType()));
				}
			}
		} else if (isStringBuilderAppend(ie)) {
			// append returns a reference to the StringBuilder object, therefore
			// parameters have to be tainted if the left side of an assignment
			// is tainted, or if the instance append is called upon is tainted.
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
			Value stringBuilderObjectCallReceiver = iie.getBase();

			boolean kill = false;
			Set<Taint> genTaints = Sets.newHashSet();

			if (callSite instanceof DefinitionStmt) {
				DefinitionStmt definitionStmt = (DefinitionStmt) callSite;
				final Value stringBuilderObjectLeftSide = definitionStmt.getLeftOp();
				if (taint.value.equals(stringBuilderObjectLeftSide)) {
					genTaints.add(taint.createAlias(stringBuilderObjectCallReceiver, callSite));
					kill = true;
				}
			}

			if (taint.value.equals(stringBuilderObjectCallReceiver) || !genTaints.isEmpty()) {
				Value stringArg = ie.getArg(0);
				if (!(stringArg instanceof Constant)) {
					genTaints.add(new Taint(callSite, taint, stringArg, stringArg.getType()));
				}
			}
			return new KillGenInfo(kill, genTaints);
		} else if (isStringBuilderConstructorCall(ie)) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
			Value stringBuilderObject = iie.getBase();
			if (taint.value.equals(stringBuilderObject)) {
				if (ie.getArgCount() == 1 && ie.getArg(0).getType().equals(RefType.v("java.lang.String"))) {
					Value arg = ie.getArg(0);
					if (!(arg instanceof Constant))
						return propagate(new Taint(callSite, taint, arg, arg.getType()));
				}

				return kill();
			}
		}
		return identity();
	}

	public static boolean isStringBuilderToString(InvokeExpr ie) {
		return ie.getMethodRef().name().equals("toString")
				&& ie.getArgCount() == 0
				&& (ie.getMethodRef().declaringClass().getName().equals("java.lang.StringBuilder") || ie.getMethodRef().declaringClass().getName()
						.equals("java.lang.StringBuffer"));
	}

	public static boolean isStringBuilderAppend(InvokeExpr ie) {
		return ie.getMethodRef().name().equals("append")
				&& ie.getArgCount() == 1
				&& (ie.getMethodRef().declaringClass().getName().equals("java.lang.StringBuilder") || ie.getMethodRef().declaringClass().getName()
						.equals("java.lang.StringBuffer"));
	}

	public static boolean isStringBuilderConstructorCall(InvokeExpr ie) {
		return ie.getMethodRef().name().equals(SootMethod.constructorName)
				&& (ie.getMethodRef().declaringClass().getName().equals("java.lang.StringBuilder") || ie.getMethodRef().declaringClass().getName()
						.equals("java.lang.StringBuffer"));
	}
}
