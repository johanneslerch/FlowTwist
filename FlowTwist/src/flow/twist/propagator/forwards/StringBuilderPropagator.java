package flow.twist.propagator.forwards;

import static flow.twist.ifds.Propagator.KillGenInfo.gen;
import static flow.twist.ifds.Propagator.KillGenInfo.identity;

import java.util.Set;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Sets;

import flow.twist.propagator.backwards.MethodFilteringPropagator;
import flow.twist.trackable.Taint;
import flow.twist.util.AnalysisUtil;

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
			if (callSite instanceof AssignStmt) {
				AssignStmt definitionStmt = (AssignStmt) callSite;
				InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
				Value stringBuilderObjectCallReceiver = iie.getBase();
				if (AnalysisUtil.maybeSameLocation(taint.value, stringBuilderObjectCallReceiver)) {
					final Value toStringReturnValue = definitionStmt.getLeftOp();
					return gen(new Taint(callSite, taint, toStringReturnValue, toStringReturnValue.getType()));
				}
			}
		} else if (isStringBuilderAppend(ie)) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
			Value stringBuilderObjectCallReceiver = iie.getBase();
			Value argValue = ie.getArg(0);

			boolean genReturn = false;
			boolean callReceiverTainted;

			if (AnalysisUtil.maybeSameLocation(taint.value, stringBuilderObjectCallReceiver)) {
				genReturn = true;
				callReceiverTainted = true;
			} else {
				callReceiverTainted = false;
			}

			if (AnalysisUtil.maybeSameLocation(taint.value, argValue)) {
				genReturn = true;
			}

			Set<Taint> genTaints = Sets.newHashSet();
			if (genReturn && callSite instanceof DefinitionStmt) {
				final Value returnValue = ((DefinitionStmt) callSite).getLeftOp();
				if (!AnalysisUtil.maybeSameLocation(taint.value, returnValue))
					genTaints.add(new Taint(callSite, taint, returnValue, returnValue.getType()));

				if (stringBuilderObjectCallReceiver.equals(returnValue))
					callReceiverTainted = true;
			}
			if (!callReceiverTainted) {
				genTaints.add(new Taint(callSite, taint, stringBuilderObjectCallReceiver, stringBuilderObjectCallReceiver.getType()));
			}
			return new KillGenInfo(false, genTaints);
		} else if (isStringBuilderConstructorCall(ie)) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
			Value stringBuilderObject = iie.getBase();
			if (ie.getArgCount() == 1) {
				Value argValue = ie.getArg(0);
				if (AnalysisUtil.maybeSameLocation(taint.value, argValue)) {
					return gen(new Taint(callSite, taint, stringBuilderObject, stringBuilderObject.getType()));
				}
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
