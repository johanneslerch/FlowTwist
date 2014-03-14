package flow.twist.pathchecker;

import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.jimple.Stmt;

@SuppressWarnings("rawtypes")
public class CalledMethod {

	private SootClass declaringClass;
	private String methodName;
	private List parameterTypes;
	private int paramIndex;

	public CalledMethod(Unit callSite, int paramIndex) {
		SootMethodRef method = ((Stmt) callSite).getInvokeExpr().getMethodRef();
		methodName = method.name();
		parameterTypes = method.parameterTypes();
		declaringClass = getDeclaringClass(method.declaringClass());
		this.paramIndex = paramIndex;
	}

	private SootClass getDeclaringClass(SootClass declaringClass) {
		for (SootClass i : declaringClass.getInterfaces()) {
			SootClass candidate = getDeclaringClass(i);
			if (candidate != null)
				return candidate;

			if (hasMethod(i))
				return i;
		}

		if (declaringClass.hasSuperclass()) {
			SootClass candidate = getDeclaringClass(declaringClass.getSuperclass());
			if (candidate != null)
				return candidate;

			if (hasMethod(declaringClass))
				return declaringClass;
		}

		return null;
	}

	private boolean hasMethod(SootClass declaringClass) {
		for (SootMethod m : declaringClass.getMethods()) {
			if (m.getName().equals(methodName) && m.getParameterTypes().equals(parameterTypes)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + paramIndex;
		result = prime * result + ((parameterTypes == null) ? 0 : parameterTypes.hashCode());
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
		CalledMethod other = (CalledMethod) obj;
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
		if (parameterTypes == null) {
			if (other.parameterTypes != null)
				return false;
		} else if (!parameterTypes.equals(other.parameterTypes))
			return false;
		return true;
	}
}