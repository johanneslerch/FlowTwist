package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;
import static flow.twist.config.AnalysisDirection.BACKWARDS;
import static flow.twist.test.util.selectors.TrackableSelectorFactory.taint;
import static flow.twist.test.util.selectors.TrackableSelectorFactory.taintContains;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;

import org.junit.Ignore;
import org.junit.Test;

import flow.twist.SolverFactory;
import flow.twist.test.util.AbstractTaintAnalysisTest;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.AnalysisGraphVerifier.UnitNode;
import flow.twist.test.util.selectors.UnitSelector;

public class BackwardsGraphTests extends AbstractTaintAnalysisTest {

	private UnitSelector forNameSelector = unitByLabel("forName");

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		SolverFactory.runOneDirectionSolver(i2oSimpleClassForNameDefaults().direction(BACKWARDS).reporter(verifier));
		return verifier;
	}

	@Test
	public void aliasing() {
		AnalysisGraphVerifier verifier = runTest("java.lang.Aliasing");
		UnitNode forName = verifier.find(unitInMethod("nameInput(", forNameSelector));
		forName.pathTo(taint("name"), unitByLabel("@parameter0"));
		forName.pathTo(taint("name3"), unitByLabel("@parameter2"));

		UnitNode forName2 = verifier.find(unitInMethod("nameInput2", forNameSelector));
		forName2.pathTo(taint("name"), unitByLabel("@parameter0"));
	}

	@Test
	@Ignore("does not work without target for forName with classloader argument")
	public void beanInstantiator() {
		AnalysisGraphVerifier verifier = runTest("java.lang.BeanInstantiator");

		UnitNode forNameSimple = verifier.find(unitInMethod("loadClass", unitByLabel("forName(java.lang.String)")));
		forNameSimple.pathTo(taint("className"), unitByLabel("@parameter0"));

		UnitNode forNameExtended = verifier.find(unitInMethod("loadClass", unitByLabel("forName(java.lang.String,")));
		forNameExtended.pathTo(taint("className"), unitByLabel("@parameter0"));
	}

	@Test
	@Ignore("does not work without target for forName with classloader argument")
	public void callerClassLoader() {
		AnalysisGraphVerifier verifier = runTest("java.lang.CallerClassLoader");
		UnitNode forName = verifier.find(unitInMethod("problematicMethod", forNameSelector));
		forName.pathTo(taint("name"), unitInMethod("leakingMethod", unitByLabel("@parameter0")));

		UnitNode okForName = verifier.find(unitInMethod("okMethod", forNameSelector));
		okForName.pathTo(taint("name"), unitByLabel("@parameter0"));
	}

	@Test
	public void checkPackageAccess() {
		AnalysisGraphVerifier verifier = runTest("java.lang.CheckPackageAccess");
		UnitNode forName = verifier.find(unitInMethod("loadIt", forNameSelector));
		forName.pathTo(taint("n"), unitInMethod("wrapperWithoutCheck", unitByLabel("@parameter0")));
	}

	@Test
	public void classHierarchyHard() {
		AnalysisGraphVerifier verifier = runTest("java.lang.ClassHierarchyHard");
		verifier.find(unitInMethod("redundantInvokeable", forNameSelector)).pathTo(taintContains("red_className"),
				unitInMethod("redundantInvokeable", unitByLabel("@parameter0")));
		verifier.find(unitInMethod("invokeable", forNameSelector)).pathTo(taintContains("inv_className"),
				unitInMethod("invokeable", unitByLabel("@parameter0")));
	}

	@Test
	public void backwardsIntoThrow() {
		AnalysisGraphVerifier verifier = runTest("java.lang.BackwardsIntoThrow");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("name"), unitInMethod("foo", unitByLabel("@parameter0")));
	}

	@Test
	public void recursion() {
		AnalysisGraphVerifier verifier = runTest("java.lang.Recursion");

		verifier.find(unitInMethod("recursive(", forNameSelector)).pathTo(taint("className"), unitByLabel("@parameter0"));

		UnitNode forName = verifier.find(unitInMethod("recursiveA", forNameSelector));
		forName.pathTo(taint("className"), unitInMethod("recursiveA", unitByLabel("@parameter0")));
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
	public void stringConcatenation() {
		AnalysisGraphVerifier verifier = runTest("java.lang.StringConcatenation");
		verifier.find(forNameSelector).pathTo(taint("name"), unitByLabel("@parameter0"));
	}

	@Test
	public void whiteboardGraph() {
		AnalysisGraphVerifier verifier = runTest("java.lang.WhiteboardGraph");
		UnitNode forName = verifier.find(forNameSelector);
		forName.pathTo(taint("s3"), unitInMethod("notVulnerableWrapper", unitByLabel("@parameter0")));
		forName.pathTo(taint("s3"), unitInMethod("vulnerable", unitByLabel("@parameter0")));
	}

	@Test
	public void mergeTest() {
		AnalysisGraphVerifier verifier = runTest("java.lang.MergeTest");
		verifier.find(unitInMethod("b(", forNameSelector)).pathTo(taint("className"), unitByLabel("@parameter0"));
		verifier.find(unitInMethod("c(", forNameSelector)).pathTo(taint("className"), unitByLabel("@parameter0"));
	}

	@Test
	public void throughGeneric() {
		AnalysisGraphVerifier verifier = runTest("generics.BackwardThroughGeneric");
		verifier.cannotFind(forNameSelector);
	}

	@Test
	public void outOfGeneric() {
		AnalysisGraphVerifier verifier = runTest("generics.BackwardOutOfGeneric");
		verifier.find(forNameSelector).pathTo(taint("name"), unitInMethod("string(", unitByLabel("@parameter0")));
		verifier.cannotFind(unitInMethod("integer(", unitByLabel("@parameter0")));
		verifier.find(forNameSelector).pathTo(taint("name"), unitInMethod("object(", unitByLabel("@parameter0")));
	}

	@Test
	public void typeConversion() {
		AnalysisGraphVerifier verifier = runTest("type.TypeConversion");
		verifier.cannotFind(forNameSelector);
	}

	@Test
	public void stringBuilderTest() {
		AnalysisGraphVerifier verifier = runTest("type.StringBuilderTest");
		verifier.find(unitInMethod("leakStringBuilder", forNameSelector)).pathTo(taint("a"), unitByLabel("@parameter0"));
		verifier.find(unitInMethod("throughStringBuilder", forNameSelector)).pathTo(taint("a"), unitByLabel("@parameter0"));
		verifier.find(unitInMethod("trackStringBuilder", forNameSelector)).pathTo(taint("b"), unitByLabel("@parameter0"));
	}
}
