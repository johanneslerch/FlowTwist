package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;
import static flow.twist.config.AnalysisDirection.FORWARDS;
import static flow.twist.test.util.selectors.TrackableSelectorFactory.taint;
import static flow.twist.test.util.selectors.TrackableSelectorFactory.taintContains;
import static flow.twist.test.util.selectors.UnitSelectorFactory.anyUnit;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitWithoutLabel;

import org.junit.Ignore;
import org.junit.Test;

import flow.twist.SolverFactory;
import flow.twist.test.util.AbstractTaintAnalysisTest;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.AnalysisGraphVerifier.UnitNode;
import flow.twist.test.util.selectors.TrackableSelector;
import flow.twist.test.util.selectors.UnitSelector;

public class ForwardsGraphTests extends AbstractTaintAnalysisTest {

	private UnitSelector forNameSelector = unitByLabel("forName");

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		SolverFactory.runOneDirectionSolver(i2oSimpleClassForNameDefaults().direction(FORWARDS).reporter(verifier));
		return verifier;
	}

	@Test
	public void aliasing() {
		AnalysisGraphVerifier verifier = runTest("java.lang.Aliasing");
		UnitNode forName = verifier.find(unitInMethod("nameInput(", forNameSelector));
		forName.pathTo(taint("$r3"), unitByLabel("return"));

		UnitNode forName2 = verifier.find(unitInMethod("nameInput2", forNameSelector));
		forName2.pathTo(taint("$r2"), unitByLabel("return"));
	}

	@Test
	@Ignore("does not work without target for forName with classloader argument")
	public void beanInstantiator() {
		AnalysisGraphVerifier verifier = runTest("java.lang.BeanInstantiator");

		UnitNode forNameSimple = verifier.find(unitInMethod("loadClass", unitByLabel("forName(java.lang.String)")));
		forNameSimple.pathTo(taintContains("$r"), unitByLabel("return"));

		UnitNode forNameExtended = verifier.find(unitInMethod("loadClass", unitByLabel("forName(java.lang.String,")));
		forNameExtended.pathTo(taintContains("$r"), unitByLabel("return"));
	}

	@Test
	@Ignore("does not work without target for forName with classloader argument")
	public void callerClassLoader() {
		AnalysisGraphVerifier verifier = runTest("java.lang.CallerClassLoader");
		UnitNode forName = verifier.find(unitInMethod("problematicMethod", forNameSelector));
		forName.pathTo(taint("$r0"), unitInMethod("leakingMethod", unitByLabel("return")));

		UnitNode okForName = verifier.find(unitInMethod("okMethod", forNameSelector));
		okForName.pathTo(taint("$r2"), unitByLabel("return"));
	}

	@Test
	public void checkPackageAccess() {
		AnalysisGraphVerifier verifier = runTest("java.lang.CheckPackageAccess");
		UnitNode forName = verifier.find(unitInMethod("loadIt", forNameSelector));
		forName.pathTo(taintContains("$r"), unitInMethod("wrapperWithCheck", unitByLabel("return")));
		forName.pathTo(taintContains("$r"), unitInMethod("wrapperWithCheck(java.lang.String", unitByLabel("return")));
		forName.pathTo(taintContains("$r"), unitInMethod("wrapperWithoutCheck", unitByLabel("return")));
		forName.pathTo(taintContains("$r"), unitInMethod("wrapperWithoutCheck(java.lang.String", unitByLabel("return")));
	}

	@Test
	public void classConstant() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ClassConstant");
		verifier.cannotFind(forNameSelector);
	}

	@Test
	public void classConstant2() {
		AnalysisGraphVerifier verifier = runTest("javax.management.remote.rmi.RMIConnectionImpl_Stub");
		verifier.cannotFind(forNameSelector);
	}

	@Test
	public void classHierarchyHard() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ClassHierarchyHard");
		verifier.find(unitInMethod("invokeable", forNameSelector)).pathTo(taint("$r0"), unitByLabel("return"));
		verifier.find(unitInMethod("redundantInvokeable", forNameSelector)).pathTo(taint("$r0"), unitByLabel("return"));
	}

	@Test
	public void classInstanceCastedBeforeReturned() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ClassInstanceCastedBeforeReturned");
		verifier.cannotFind(unitInMethod("newInstance", unitByLabel("return")));
		verifier.cannotFind(unitInMethod("explicitConstructor", unitByLabel("return")));
		verifier.cannotFind(unitInMethod("allConstructors", unitByLabel("return")));
	}

	@Test
	public void classInstanceReturned() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ClassInstanceReturned");
		verifier.find(unitInMethod("newInstance", forNameSelector)).pathTo(taint("result"), unitByLabel("return"));
		verifier.find(unitInMethod("explicitConstructor", forNameSelector)).pathTo(taint("result"), unitByLabel("return"));
		verifier.find(unitInMethod("allConstructors", forNameSelector)).pathTo(taint("result"), unitByLabel("return"));
	}

	@Test
	public void distinguishPaths() {
		AnalysisGraphVerifier verifier = runTest("java.lang.DistinguishPaths");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("checkedResult"), unitInMethod("leakingMethod", unitByLabel("return")));

		verifier.cannotFind(unitInMethod("okMethod", unitByLabel("return")));
	}

	@Test
	public void exceptionalPath() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ExceptionalPath");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("$r0"), unitInMethod("test", unitByLabel("return")));

		verifier.cannotFind(unitByLabel("return null"));
		verifier.cannotFind(unitByLabel("throw"));
	}

	@Test
	public void privateMethod() {
		AnalysisGraphVerifier verifier = runTest("java.lang.PrivateMethod");
		verifier.find(forNameSelector).pathTo(taint("$r0"), unitInMethod("leakingMethod", unitByLabel("return")));
	}

	@Test
	public void recursion() {
		AnalysisGraphVerifier verifier = runTest("java.lang.Recursion");
		{
			UnitNode forName = verifier.find(unitInMethod("recursive(", forNameSelector));
			forName.pathTo(taint("result"), unitByLabel("return result"));
			forName.pathTo(taint("recResult"), unitByLabel("return recResult"));
		}
		{
			TrackableSelector t = taint("$r0");
			UnitNode forName = verifier.find(unitInMethod("recursiveA", forNameSelector));
			UnitNode retAinB = forName.edge(t, unitByLabel("return $r0")).edge(t, unitInMethod("recursiveB", unitByLabel("return $r0")));
			retAinB.edge(t, unitInMethod("recursiveA", unitByLabel("return $r0")));
			UnitNode retBinB = retAinB.edge(t, unitInMethod("recursiveB", unitByLabel("return $r0")));
			UnitNode retBinA = retBinB.edge(t, unitInMethod("recursiveA", unitByLabel("return $r0")));
			retBinA.edge(t, unitInMethod("recursiveB", unitByLabel("return $r0")));
		}
	}

	@Test
	public void returnEdgeMerge() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ReturnEdgeMerge");
		verifier.find(unitWithoutLabel("goto", unitByLabel("return result"))).assertIncomingEdges(3);
	}

	@Test
	public void recursionAndClassHierarchy() {
		AnalysisGraphVerifier verifier = runTest("java.lang.RecursionAndClassHierarchy", "java.lang.RecursionAndClassHierarchy$BaseInterface",
				"java.lang.RecursionAndClassHierarchy$SubClassA", "java.lang.RecursionAndClassHierarchy$SubClassB",
				"java.lang.RecursionAndClassHierarchy$SubClassC");
		verifier.find(forNameSelector);
		// Just be happy that it terminates here.
	}

	@Test
	public void recursionClassAsParameter() {
		AnalysisGraphVerifier verifier = runTest("java.lang.RecursionClassAsParameter");
		verifier.find(forNameSelector).pathTo(taint("c"), unitByLabel("return c"));
	}

	@Test
	public void validPathCheck() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ValidPathCheck");
		verifier.find(forNameSelector).pathTo(taint("$r0"), unitInMethod("a", unitByLabel("return")));
	}

	@Test
	public void whiteboardGraph() {
		AnalysisGraphVerifier verifier = runTest("java.lang.WhiteboardGraph");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("r2"), unitInMethod("notVulnerable", unitByLabel("return r2")));
		verifier.cannotFind(unitInMethod("notVulnerable", unitByLabel("return r1")));
		forName.pathTo(taint("r3"), unitInMethod("vulnerable", unitByLabel("return")));
	}

	@Test
	public void javaUtil() {
		AnalysisGraphVerifier verifier = runTest("java.lang.JavaUtil", "java.util.HashMap", "java.util.Map", "java.lang.JavaUtil$CustomMapA",
				"java.lang.JavaUtil$CustomMapB");
		verifier.cannotFind(unitInMethod("put", anyUnit()));
	}

	@Test
	public void impossiblePath() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ImpossiblePath");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("b"), unitByLabel("return"));
	}

	@Test
	public void throughGeneric() {
		AnalysisGraphVerifier verifier = runTest("generics.ForwardThroughGeneric");
		verifier.find(forNameSelector).pathTo(taint("result"), unitInMethod("foo", unitByLabel("return")));
		verifier.cannotFind(unitInMethod("$C", anyUnit()));
	}

	@Test
	public void outOfGeneric() {
		AnalysisGraphVerifier verifier = runTest("generics.ForwardOutOfGeneric");
		verifier.find(forNameSelector).pathTo(taintContains("$r"), unitInMethod("clazz(", unitByLabel("return")));
		verifier.cannotFind(unitInMethod("integer(", unitByLabel("return")));
		verifier.find(forNameSelector).pathTo(taintContains("$r"), unitInMethod("object(", unitByLabel("return")));
	}
}
