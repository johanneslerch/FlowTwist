package flow.twist.targets;

import static fj.data.Option.none;
import static fj.data.Option.some;
import static flow.twist.config.AnalysisDirection.BACKWARDS;
import static flow.twist.config.AnalysisDirection.FORWARDS;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import static flow.twist.ifds.Propagator.KillGenInfo.propagate;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.PrimType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fj.data.Option;
import flow.twist.config.AnalysisConfiguration;
import flow.twist.config.AnalysisDirection;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.path.Path;
import flow.twist.pathchecker.PathChecker;
import flow.twist.pathchecker.StoreAnyPath;
import flow.twist.pathchecker.SubPathRemover;
import flow.twist.pathchecker.TwoPathChecker;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import flow.twist.util.CacheMap;

public class GenericCallerSensitive extends AnalysisTarget {

	private Set<SootMethod> sensitiveMethods = Sets.newHashSet();
	private CacheMap<SootMethod, PathChecker> pathChecker = new CacheMap<SootMethod, PathChecker>() {
		@Override
		protected PathChecker createItem(SootMethod method) {
			if (matches(FORWARDS, method) && matches(BACKWARDS, method)) {
				return new TwoPathChecker(new SubPathRemover(new StoreAnyPath(GenericCallerSensitive.this)));
			} else {
				return new StoreAnyPath(GenericCallerSensitive.this);
			}
		}
	};

	public GenericCallerSensitive() {
		for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
			SootMethod m = iter.next().method();
			for (Tag tag : m.getTags()) {
				if (tag instanceof VisibilityAnnotationTag) {
					for (AnnotationTag annotation : ((VisibilityAnnotationTag) tag).getAnnotations()) {
						if (annotation.getType().equals("Lsun/reflect/CallerSensitive;")) {
							if (hasTaintableValueForDirection(FORWARDS, m) && hasTaintableValueForDirection(BACKWARDS, m))
								sensitiveMethods.add(m);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean matches(AnalysisDirection direction, SootMethod enclosingMethod, InvokeExpr ie) {
		return matches(direction, ie.getMethod());
	}

	private boolean matches(AnalysisDirection direction, SootMethod method) {
		if (!sensitiveMethods.contains(method))
			return false;

		return hasTaintableValueForDirection(direction, method);
	}

	protected boolean hasTaintableValueForDirection(AnalysisDirection direction, SootMethod method) {
		if (direction == BACKWARDS) {
			if (!method.isStatic())
				return true;

			for (Type parameterType : method.getParameterTypes()) {
				if (!(parameterType instanceof PrimType)) {
					return true;
				}
			}
			return false;
		} else {
			return !(method.getReturnType() instanceof VoidType);
		}
	}

	@Override
	public KillGenInfo processForwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie, Value left) {
		return propagate(new Taint(callSite, taint, left, ie.getMethod().getReturnType()));
	}

	@Override
	public KillGenInfo processBackwardCallToReturn(Trackable taint, Stmt callSite, InvokeExpr ie) {
		SootMethod method = ie.getMethod();
		List<Taint> taints = Lists.newLinkedList();
		if (ie instanceof InstanceInvokeExpr) {
			Value receiver = ((InstanceInvokeExpr) ie).getBase();
			taints.add(new Taint(callSite, taint, receiver, receiver.getType()));
		}

		for (int i = 0; i < method.getParameterCount(); i++) {
			Type parameterType = method.getParameterType(i);
			if (!(parameterType instanceof PrimType) && !(ie.getArg(i) instanceof Constant)) {
				taints.add(new Taint(callSite, taint, ie.getArg(i), parameterType));
			}
		}

		if (taints.isEmpty())
			return kill();
		else
			return propagate(taints.toArray(new Taint[taints.size()]));
	}

	@Override
	public void enableIfDeferred() {
	}

	@Override
	public Option<PathChecker> getPathChecker(Path path) {
		if (path.context.type != AnalysisConfiguration.Type.InnerToOuter)
			return super.getPathChecker(path);

		Stmt stmt = (Stmt) path.getFirst();
		if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			SootMethod enclosingMethod = path.context.icfg.getMethodOf(stmt);
			if (matches(path.context.direction, enclosingMethod, ie)) {
				return some(pathChecker.getOrCreate(ie.getMethod()));
			}
		}
		return none();
	}

	public Set<SootMethod> getSensitiveMethods() {
		return sensitiveMethods;
	}
}
