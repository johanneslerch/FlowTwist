package flow.twist.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.targets.AnalysisTarget;

public class AnalysisUtil {

	/**
	 * Returns <code>true</code> if both references may point to the same memory
	 * location. The current implementation is conservative, as it does not take
	 * any points-to information into account.
	 */
	public static boolean maybeSameLocation(Value abstrRef, Value programRef) {
		// arrays are handled through their "base" pointer
		assert !(abstrRef instanceof ArrayRef);
		assert !(programRef instanceof ArrayRef);

		if (abstrRef == programRef)
			return true;

		// handle primtive types
		Type abstrRefType = abstrRef.getType();
		Type programRefType = programRef.getType();
		if (programRefType instanceof PrimType) {
			// we don't track primitive types, just Strings, ClassLoaders, etc.
			// ...
			return false;
		}
		if (abstrRefType instanceof PrimType) {
			// we don't track primitive types, just Strings, ClassLoaders, etc.
			// ...
			throw new InternalError("abstraction ref type is " + abstrRefType);
		}

		if (abstrRef instanceof Local && programRef instanceof Local) {
			// two locals only point to the same memory locations if they are
			// the same
			return abstrRef == programRef;
		} else if (abstrRef instanceof FieldRef && programRef instanceof FieldRef) {
			FieldRef fieldRef = (FieldRef) abstrRef;
			FieldRef fieldRef2 = (FieldRef) programRef;
			// references point to the same location if class and field name are
			// identical;
			// note that we ignore the receiver object of InstanceFieldRefs
			return fieldRef.getField().getDeclaringClass().equals(fieldRef2.getField().getDeclaringClass())
					&& fieldRef.getFieldRef().name().equals(fieldRef2.getFieldRef().name());
		} else {
			return false;
		}
	}

	public static String methodCanBeOverwrittenByApplication(SootMethod m) {
		int methodModifiers = m.getModifiers();
		int classModifiers = m.getDeclaringClass().getModifiers();

		if (Modifier.isPublic(classModifiers) && !Modifier.isFinal(classModifiers)) {
			if (Modifier.isPublic(methodModifiers) && !Modifier.isStatic(methodModifiers) && !Modifier.isFinal(methodModifiers)) {
				return "Method and declaring class are public and non-final, method is non-static.";
			} else if (Modifier.isProtected(methodModifiers) && !Modifier.isStatic(methodModifiers) && !Modifier.isFinal(methodModifiers)) {
				return "Method is protected and non-final and non-static, declaring class is public and non-final.";
			}
		}

		return null;
	}

	private static String[] restrictedPackages;

	public static void initRestrictedPackages(String pathToJREHome) {
		Properties secProps = new Properties();
		try {
			try {
				secProps.load(new InputStreamReader(new FileInputStream(pathToJREHome + File.separator + "lib" + File.separator + "security"
						+ File.separator + "java.security")));
			} catch (Exception e) {
				secProps.load(new InputStreamReader(new FileInputStream(pathToJREHome + File.separator + "jre" + File.separator + "lib"
						+ File.separator + "security" + File.separator + "java.security")));
			}
			restrictedPackages = secProps.getProperty("package.access").split(",");
			String defn = secProps.getProperty("package.definition");
			if (defn != null) {
				String[] defnPackages = defn.split(",");
				if (!Arrays.equals(restrictedPackages, defnPackages)) {
					System.err.println("package.access differs from package.definition!");
				}
			}

			System.out.println("The following packages are access restricted:");
			for (int i = 0; i < restrictedPackages.length; i++) {
				restrictedPackages[i] = restrictedPackages[i].trim();
				System.out.println(restrictedPackages[i]);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isAccessRestricted(SootClass sc) {
		for (String pkg : restrictedPackages) {
			String name = sc.getName();
			if (name.startsWith(pkg)) {
				return true;
			}
		}
		return false;
	}

	public static int getLine(Unit unit) {
		SourceLnPosTag tag = (SourceLnPosTag) unit.getTag("SourceLnPosTag");
		if (tag != null)
			return tag.startLn();
		LineNumberTag lnTag = (LineNumberTag) unit.getTag("LineNumberTag");
		if (lnTag != null)
			return lnTag.getLineNumber();
		return -1;
	}

	public static boolean isTarget(AnalysisContext context, Unit unit) {
		return isTarget(context.direction, context.icfg.getMethodOf(unit), unit, context.targets);
	}

	public static boolean isTarget(AnalysisDirection direction, SootMethod enclosingMethod, Unit unit, Iterable<AnalysisTarget> targets) {
		if (!(unit instanceof AssignStmt))
			return false;

		Stmt s = (Stmt) unit;
		if (!s.containsInvokeExpr())
			return false;

		InvokeExpr ie = s.getInvokeExpr();
		for (AnalysisTarget target : targets) {
			if (target.matches(direction, enclosingMethod, ie))
				return true;
		}
		return false;
	}

	public static boolean methodMayBeCallableFromApplication(SootMethod m) {
		int methodModifiers = m.getModifiers();
		int classModifiers = m.getDeclaringClass().getModifiers();

		if (Modifier.isPublic(classModifiers) && !isAccessRestricted(m.getDeclaringClass())) {
			if (Modifier.isPublic(methodModifiers)) {
				return true;
			} else if (Modifier.isProtected(methodModifiers) && !Modifier.isFinal(classModifiers)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * extracts array base values from array refs and values from cast
	 * expressions
	 */
	public static Value getBackwardsBase(Value val) {
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

	public static Value getForwardsBase(Value val) {
		if (val instanceof ArrayRef) {
			ArrayRef arrayRef = (ArrayRef) val;
			return arrayRef.getBase();
		}
		return val;
	}

	/**
	 * Returns true if an instance of type taintType is assignable to a
	 * declaration of type declaredType.
	 */
	public static boolean isAssignable(Type taintType, Type declaredType) {
		if (taintType instanceof ArrayType)
			taintType = ((ArrayType) taintType).getArrayElementType();
		if (declaredType instanceof ArrayType)
			declaredType = ((ArrayType) declaredType).getArrayElementType();
		if (taintType.equals(declaredType))
			return true;
		if (!(taintType instanceof RefType))
			return false;
		if (!(declaredType instanceof RefType)) {
			return false;
		}
		return isAssignable(((RefType) taintType).getSootClass(), ((RefType) declaredType).getSootClass());
	}

	public static boolean isAssignable(SootClass taintType, SootClass declaredType) {
		if (taintType.equals(declaredType))
			return true;

		if (taintType.hasSuperclass() && isAssignable(taintType.getSuperclass(), declaredType)) {
			return true;
		}
		for (SootClass interf : taintType.getInterfaces()) {
			if (isAssignable(interf, declaredType))
				return true;
		}
		return false;
	}

	public static Set<SootMethod> getInitialDeclaration(SootMethod method) {
		return getInitialDeclaration(method, method.getDeclaringClass());
	}

	// TODO: Consider implementing a cached version
	private static Set<SootMethod> getInitialDeclaration(SootMethod method, SootClass candidate) {
		Set<SootMethod> result = Sets.newHashSet();

		if (candidate.hasSuperclass()) {
			result.addAll(getInitialDeclaration(method, candidate.getSuperclass()));
		}
		for (SootClass intface : candidate.getInterfaces()) {
			result.addAll(getInitialDeclaration(method, intface));
		}

		if (result.isEmpty()) {
			for (SootMethod candidateMethod : candidate.getMethods()) {
				if (compatible(candidateMethod, method)) {
					result.add(candidateMethod);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Returns true, if subClassMethod could be overriding superClassMethod.
	 * Check only considers the method name and the parameters.
	 */
	public static boolean compatible(SootMethod superClassMethod, SootMethod subClassMethod) {
		if (!superClassMethod.getName().equals(subClassMethod.getName()))
			return false;

		if (superClassMethod.getParameterCount() != subClassMethod.getParameterCount())
			return false;

		boolean paramsMatch = true;
		for (int i = 0; i < superClassMethod.getParameterCount(); i++) {
			Type superType = superClassMethod.getParameterType(i);
			Type subType = superClassMethod.getParameterType(i);
			if (!isAssignable(superType, subType)) {
				paramsMatch = false;
				break;
			}
		}

		return paramsMatch;
	}
}
