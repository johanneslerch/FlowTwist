package flow.twist.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import flow.twist.test.util.AbstractAnalysisTest;
import flow.twist.util.AnalysisUtil;

public class AnalysisUtilTest extends AbstractAnalysisTest {

	@Test
	public void assignableToSuperClass() {
		runAnalysis(new TestAnalysis("analysisutil.SubTypeTests") {
			@Override
			protected void executeAnalysis() {
				RefType typeA = Scene.v().getSootClass("analysisutil.SubTypeTests$A").getType();
				RefType typeB = Scene.v().getSootClass("analysisutil.SubTypeTests$B").getType();

				assertTrue(AnalysisUtil.isAssignable(typeB, typeA));
				assertFalse(AnalysisUtil.isAssignable(typeA, typeB));
			}
		});
	}

	@Test
	public void assignableToInterface() {
		runAnalysis(new TestAnalysis("analysisutil.SubTypeTests") {
			@Override
			protected void executeAnalysis() {
				RefType typeI = Scene.v().getSootClass("analysisutil.SubTypeTests$I").getType();
				RefType typeJ = Scene.v().getSootClass("analysisutil.SubTypeTests$J").getType();

				assertTrue(AnalysisUtil.isAssignable(typeJ, typeI));
				assertFalse(AnalysisUtil.isAssignable(typeI, typeJ));
			}
		});
	}

	@Test
	public void assignableToIdentity() {
		runAnalysis(new TestAnalysis("analysisutil.SubTypeTests") {
			@Override
			protected void executeAnalysis() {
				RefType typeA = Scene.v().getSootClass("analysisutil.SubTypeTests$A").getType();
				RefType typeB = Scene.v().getSootClass("analysisutil.SubTypeTests$B").getType();
				RefType typeI = Scene.v().getSootClass("analysisutil.SubTypeTests$I").getType();
				RefType typeJ = Scene.v().getSootClass("analysisutil.SubTypeTests$J").getType();

				assertTrue(AnalysisUtil.isAssignable(typeA, typeA));
				assertTrue(AnalysisUtil.isAssignable(typeB, typeB));
				assertTrue(AnalysisUtil.isAssignable(typeI, typeI));
				assertTrue(AnalysisUtil.isAssignable(typeJ, typeJ));
			}
		});
	}

	@Test
	public void unassignableToDifferentHierarchy() {
		runAnalysis(new TestAnalysis("analysisutil.SubTypeTests") {
			@Override
			protected void executeAnalysis() {
				RefType typeA = Scene.v().getSootClass("analysisutil.SubTypeTests$A").getType();
				RefType typeB = Scene.v().getSootClass("analysisutil.SubTypeTests$B").getType();
				RefType typeI = Scene.v().getSootClass("analysisutil.SubTypeTests$I").getType();
				RefType typeJ = Scene.v().getSootClass("analysisutil.SubTypeTests$J").getType();

				assertFalse(AnalysisUtil.isAssignable(typeA, typeI));
				assertFalse(AnalysisUtil.isAssignable(typeA, typeJ));
				assertFalse(AnalysisUtil.isAssignable(typeB, typeI));
				assertFalse(AnalysisUtil.isAssignable(typeB, typeJ));
			}
		});
	}

	@Test
	public void initDeclInInterface() {
		runAnalysis(new TestAnalysis("analysisutil.InitiallyDeclaredMethodTests") {
			@Override
			protected void executeAnalysis() {
				SootMethod method = Scene.v().getSootClass("analysisutil.InitiallyDeclaredMethodTests$A").getMethod("void inInterface()");
				Set<SootMethod> set = AnalysisUtil.getInitialDeclaration(method);
				assertEquals(1, set.size());
				assertTrue(set.iterator().next().toString().contains("$I"));
			}
		});
	}

	@Test
	public void initDeclInInterfaceOfSupertype() {
		runAnalysis(new TestAnalysis("analysisutil.InitiallyDeclaredMethodTests") {
			@Override
			protected void executeAnalysis() {
				SootMethod method = Scene.v().getSootClass("analysisutil.InitiallyDeclaredMethodTests$B").getMethod("void inInterface()");
				Set<SootMethod> set = AnalysisUtil.getInitialDeclaration(method);
				assertEquals(1, set.size());
				assertTrue(set.iterator().next().toString().contains("$I"));
			}
		});
	}

	@Test
	public void initDeclInExtendedInterfaceOfSupertype() {
		runAnalysis(new TestAnalysis("analysisutil.InitiallyDeclaredMethodTests") {
			@Override
			protected void executeAnalysis() {
				SootMethod method = Scene.v().getSootClass("analysisutil.InitiallyDeclaredMethodTests$C").getMethod("void inInterface()");
				Set<SootMethod> set = AnalysisUtil.getInitialDeclaration(method);
				assertEquals(1, set.size());
				assertTrue(set.iterator().next().toString().contains("$I"));
			}
		});
	}

	@Test
	public void initDeclInSupertype() {
		runAnalysis(new TestAnalysis("analysisutil.InitiallyDeclaredMethodTests") {
			@Override
			protected void executeAnalysis() {
				SootMethod method = Scene.v().getSootClass("analysisutil.InitiallyDeclaredMethodTests$B").getMethod("void inSuperType()");
				Set<SootMethod> set = AnalysisUtil.getInitialDeclaration(method);
				assertEquals(1, set.size());
				assertTrue(set.iterator().next().toString().contains("$A"));
			}
		});
	}

	@Test
	public void initDeclInSupertypeWithGap() {
		runAnalysis(new TestAnalysis("analysisutil.InitiallyDeclaredMethodTests") {
			@Override
			protected void executeAnalysis() {
				SootMethod method = Scene.v().getSootClass("analysisutil.InitiallyDeclaredMethodTests$E").getMethod("void inSuperType()");
				Set<SootMethod> set = AnalysisUtil.getInitialDeclaration(method);
				assertEquals(1, set.size());
				assertTrue(set.iterator().next().toString().contains("$A"));
			}
		});
	}
}
