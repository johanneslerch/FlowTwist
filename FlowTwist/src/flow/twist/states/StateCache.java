package flow.twist.states;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.Stmt;

import com.google.common.collect.Maps;

import flow.twist.config.AnalysisContext;
import flow.twist.trackable.ReturnEdgeTaint;
import flow.twist.trackable.Trackable;
import flow.twist.util.CacheMap;

public class StateCache {

	private CacheMap<AnalysisContext, ContextStateCache> instances = new CacheMap<AnalysisContext, ContextStateCache>() {
		@Override
		protected ContextStateCache createItem(AnalysisContext key) {
			return new ContextStateCache(key);
		}
	};

	public ContextStateCache get(AnalysisContext context) {
		return instances.getOrCreate(context);
	}

	public Collection<ContextStateCache> getAll() {
		return instances.values();
	}

	public static class ContextStateCache {

		private HashMap<StatePushNode, StatePushNode> pushCache = Maps.newHashMap();
		private IdentityHashMap<Trackable, StatePopNode> popCache = Maps.newIdentityHashMap();
		private Map<SootMethod, StateStartNode> startNodes = Maps.newHashMap();
		public final AnalysisContext context;
		private Map<Unit, StateSinkNode> sinkNodes = Maps.newHashMap();

		private ContextStateCache(AnalysisContext context) {
			this.context = context;
		}

		public StateMetadataWrapper<StatePushNode> getOrCreatePushState(ReturnEdgeTaint taint) {
			SootMethodRef method = ((Stmt) taint.callSite).getInvokeExpr().getMethodRef();
			SootClass declaringClass = getDeclaringClass(method.declaringClass(), method);
			StatePushNode temp = new StatePushNode(context, declaringClass, method.name(), taint.paramIndex, method.parameterTypes());
			if (pushCache.containsKey(temp))
				return new StateMetadataWrapper<StatePushNode>(false, pushCache.get(temp));
			else {
				pushCache.put(temp, temp);
				return new StateMetadataWrapper<StatePushNode>(true, temp);
			}
		}

		private static SootClass getDeclaringClass(SootClass declaringClass, SootMethodRef method) {
			for (SootClass i : declaringClass.getInterfaces()) {
				SootClass candidate = getDeclaringClass(i, method);
				if (candidate != null)
					return candidate;

				if (hasMethod(i, method))
					return i;
			}

			if (declaringClass.hasSuperclass()) {
				SootClass candidate = getDeclaringClass(declaringClass.getSuperclass(), method);
				if (candidate != null)
					return candidate;

				if (hasMethod(declaringClass, method))
					return declaringClass;
			}

			return null;
		}

		private static boolean hasMethod(SootClass declaringClass, SootMethodRef method) {
			for (SootMethod m : declaringClass.getMethods()) {
				if (m.getName().equals(method.name()) && m.getParameterTypes().equals(method.parameterTypes())) {
					return true;
				}
			}
			return false;
		}

		public StateMetadataWrapper<StatePopNode> getOrCreatePopState(Trackable trackable) {
			if (popCache.containsKey(trackable))
				return new StateMetadataWrapper<StatePopNode>(false, popCache.get(trackable));
			else {
				StatePopNode temp = new StatePopNode(context, trackable);
				popCache.put(trackable, temp);
				return new StateMetadataWrapper<StatePopNode>(true, temp);
			}
		}

		public Collection<StatePopNode> getAllPopStates() {
			return popCache.values();
		}

		public Collection<StateStartNode> getAllStartStates() {
			return startNodes.values();
		}

		public StateNode getOrCreateStartState(SootMethod startMethod) {
			if (startNodes.containsKey(startMethod))
				return startNodes.get(startMethod);

			StateStartNode state = new StateStartNode(context, startMethod);
			startNodes.put(startMethod, state);
			return state;
		}

		public StateSinkNode getSinkState(Unit sinkUnit) {
			if (sinkNodes.containsKey(sinkUnit))
				return sinkNodes.get(sinkUnit);
			else {
				StateSinkNode stateSinkNode = new StateSinkNode(context, sinkUnit);
				sinkNodes.put(sinkUnit, stateSinkNode);
				return stateSinkNode;
			}
		}

	}

	public static class StateStartNode extends StateNode {

		public final SootMethod method;

		public StateStartNode(AnalysisContext context, SootMethod method) {
			super(context);
			this.method = method;
		}
	}

	public static class StateSinkNode extends StateNode {

		public final Unit unit;

		public StateSinkNode(AnalysisContext context, Unit unit) {
			super(context);
			this.unit = unit;
		}

	}

	public static class StatePushNode extends StateNode {

		public final SootClass declaringClass;
		public final String methodName;
		public final int paramIndex;
		public final List paramTypes;

		private StatePushNode(AnalysisContext context, SootClass declaringClass, String methodName, int paramIndex, List paramTypes) {
			super(context);
			this.declaringClass = declaringClass;
			this.methodName = methodName;
			this.paramIndex = paramIndex;
			this.paramTypes = paramTypes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
			result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
			result = prime * result + paramIndex;
			result = prime * result + ((paramTypes == null) ? 0 : paramTypes.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StatePushNode other = (StatePushNode) obj;
			if (declaringClass == null) {
				if (other.declaringClass != null)
					return false;
			} else if (!declaringClass.equals(other.declaringClass))
				return false;
			if (methodName == null) {
				if (other.methodName != null)
					return false;
			} else if (!methodName.equals(other.methodName))
				return false;
			if (paramIndex != other.paramIndex)
				return false;
			if (paramTypes == null) {
				if (other.paramTypes != null)
					return false;
			} else if (!paramTypes.equals(other.paramTypes))
				return false;
			return true;
		}
	}

	public static class StatePopNode extends StateNode {

		private Trackable trackable;

		private StatePopNode(AnalysisContext context, Trackable trackable) {
			super(context);
			this.trackable = trackable;
		}
	}

	public static class StateMetadataWrapper<T> {
		private boolean created;
		private T data;

		private StateMetadataWrapper(boolean created, T data) {
			this.created = created;
			this.data = data;
		}

		public boolean wasCreated() {
			return created;
		}

		public T get() {
			return data;
		}
	}
}
