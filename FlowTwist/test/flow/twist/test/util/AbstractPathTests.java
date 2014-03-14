package flow.twist.test.util;

import static flow.twist.test.util.selectors.UnitSelectorFactory.anyUnit;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitWithoutLabel;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import flow.twist.test.util.PathVerifier.PathSelector;
import flow.twist.util.MultipleAnalysisPlotter;

public abstract class AbstractPathTests extends AbstractTaintAnalysisTest {

	protected PathVerifier pathVerifier;

	@Rule
	public TestWatcher watcher = new TestWatcher() {
		@Override
		public void failed(Throwable e, Description description) {
			StringBuilder builder = new StringBuilder();
			builder.append("tmp/");
			builder.append(AbstractPathTests.this.getClass().getSimpleName());
			builder.append("_");
			builder.append(description.getMethodName());
			builder.append("_path");

			MultipleAnalysisPlotter plotter = new MultipleAnalysisPlotter();
			plotter.plotAnalysisResults(pathVerifier.getPaths(), "red");
			plotter.writeFile(builder.toString());
		}
	};

	@Test
	public void aliasing() {
		runTest("java.lang.Aliasing");
		pathVerifier.totalPaths(3);
		pathVerifier.startsAt(unitInMethod("nameInput(", unitByLabel("@parameter0"))).endsAt(unitByLabel("return")).once();
		pathVerifier.startsAt(unitInMethod("nameInput(", unitByLabel("@parameter2"))).endsAt(unitByLabel("return")).once();
		pathVerifier.startsAt(unitInMethod("nameInput2(", unitByLabel("@parameter0"))).endsAt(unitByLabel("return")).once();
	}

	@Test
	public void backwardsIntoThrow() {
		runTest("java.lang.BackwardsIntoThrow");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("foo", unitByLabel("@parameter0"))).endsAt(unitInMethod("foo", unitByLabel("return"))).once();
	}

	@Test
	@Ignore("does not work without target for forName with classloader argument")
	public void beanInstantiator() {
		runTest("java.lang.BeanInstantiator");
		pathVerifier.totalPaths(3);
		PathSelector path = pathVerifier.startsAt(unitInMethod("findClass", unitByLabel("@parameter0"))).endsAt(
				unitInMethod("findClass", unitByLabel("return")));
		path.contains(unitWithoutLabel("goto", unitByLabel("forName(java.lang.String,boolean,java.lang.ClassLoader)"))).once();
		path.contains(unitWithoutLabel("goto", unitByLabel("forName(java.lang.String)"))).once();
	}

	@Test
	public void checkPackageAccess() {
		runTest("java.lang.CheckPackageAccess");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("wrapperWithoutCheck(java.lang.String)", unitByLabel("@parameter0")))
				.endsAt(unitInMethod("wrapperWithoutCheck(java.lang.String)", unitByLabel("return"))).contains(unitByLabel("forName")).once();
	}

	@Test
	@Ignore("doPrivileged is native; we have no special handling yet")
	public void doPrivileged() {
		runTest("java.lang.DoPrivileged", "java.lang.DoPrivileged$1", "java.lang.DoPrivileged$2");
		pathVerifier.totalPaths(2);
		pathVerifier.startsAt(unitInMethod("callable1", unitByLabel("@parameter0"))).endsAt(unitInMethod("callable1", unitByLabel("return"))).once();
		pathVerifier.startsAt(unitInMethod("callable2", unitByLabel("@parameter0"))).endsAt(unitInMethod("callable2", unitByLabel("return"))).once();
	}

	@Test
	public void classHierarchyHard() {
		runTest("java.lang.ClassHierarchyHard");
		pathVerifier.totalPaths(2);
		pathVerifier.startsAt(unitInMethod("invokeable", unitByLabel("@parameter0"))).endsAt(unitInMethod("invokeable", unitByLabel("return")))
				.once();
		pathVerifier.startsAt(unitInMethod("redundantInvokeable", unitByLabel("@parameter0")))
				.endsAt(unitInMethod("redundantInvokeable", unitByLabel("return"))).once();
	}

	@Test
	public void classHierarchySimple() {
		runTest("java.lang.ClassHierarchySimple");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("foo", unitByLabel("@parameter0"))).endsAt(unitInMethod("foo", unitByLabel("return")))
				.contains(unitInMethod("$B: java.lang.Class test", unitByLabel("forName"))).once();
	}

	@Test
	public void distinguishPaths() {
		runTest("java.lang.DistinguishPaths");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("leakingMethod", unitByLabel("@parameter0"))).endsAt(unitInMethod("leakingMethod", unitByLabel("return")))
				.once();
	}

	@Test
	public void loop() {
		runTest("java.lang.Loop");
		pathVerifier.startsAt(unitByLabel("@parameter0")).endsAt(unitByLabel("return")).times(1);
	}

	@Test
	@Ignore("not implemented")
	public void permissionCheckNotOnCallstack() {
		runTest("java.lang.PermissionCheckNotOnCallstack");
		pathVerifier.totalPaths(0);
	}

	@Test
	public void permissionCheckNotOnAllPaths() {
		runTest("java.lang.PermissionCheckNotOnAllPaths");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitByLabel("@parameter0")).endsAt(unitByLabel("return")).doesNotContain(unitByLabel("name = className")).once();
	}

	@Test
	public void recursion() {
		runTest("java.lang.Recursion");
		pathVerifier.totalPaths(2);

		pathVerifier.startsAt(unitInMethod("recursive(", unitByLabel("@parameter0"))).endsAt(unitInMethod("recursive(", unitByLabel("return")))
				.once();
		pathVerifier.startsAt(unitInMethod("recursiveA(", unitByLabel("@parameter0"))).endsAt(unitInMethod("recursiveA(", unitByLabel("return")))
				.once();
	}

	@Test
	public void recursionAndClassHierarchy() {
		runTest("java.lang.RecursionAndClassHierarchy", "java.lang.RecursionAndClassHierarchy$BaseInterface",
				"java.lang.RecursionAndClassHierarchy$SubClassA", "java.lang.RecursionAndClassHierarchy$SubClassB",
				"java.lang.RecursionAndClassHierarchy$SubClassC");

		pathVerifier.totalPaths(4);
	}

	@Test
	public void recursionAndClassAsParameter() {
		runTest("java.lang.RecursionClassAsParameter");
		pathVerifier.totalPaths(1);
	}

	@Test
	public void sourceOnCallstack() {
		runTest("java.lang.SourceOnCallstack");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("baz", unitByLabel("@parameter0"))).endsAt(unitInMethod("baz", unitByLabel("return")))
				.contains(unitByLabel("forName")).once();
	}

	@Test
	public void stringConcatenation() {
		runTest("java.lang.StringConcatenation");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitByLabel("@parameter0")).endsAt(unitByLabel("return")).contains(unitByLabel("forName")).once();
	}

	@Test
	public void whileSwitch() {
		runTest("java.lang.Switch");
		pathVerifier.totalPaths(5);
	}

	@Test
	public void validPathCheck() {
		runTest("java.lang.ValidPathCheck");
		pathVerifier.totalPaths(1);
		pathVerifier.startsAt(unitInMethod("a(", unitByLabel("@parameter0"))).endsAt(unitInMethod("a(", unitByLabel("return")))
				.doesNotContain(unitInMethod("e(", anyUnit())).once();
	}

	@Test
	public void whiteboardGraph() {
		runTest("java.lang.WhiteboardGraph");
		pathVerifier.totalPaths(1);

		PathSelector vulnerablePathSelector = pathVerifier.startsAt(unitInMethod("vulnerable", unitByLabel("@parameter0"))).endsAt(
				unitInMethod("vulnerable", unitByLabel("return")));
		vulnerablePathSelector.once();

		PathSelector notVulnerablePathSelector = pathVerifier.startsAt(unitInMethod("notVulnerable", unitByLabel("@parameter0"))).endsAt(
				unitInMethod("notVulnerable", unitByLabel("return")));
		notVulnerablePathSelector.never();
	}

	@Test
	public void impossiblePath() {
		runTest("java.lang.ImpossiblePath");
		pathVerifier.startsAt(unitByLabel("@parameter0")).endsAt(unitByLabel("return")).once();
	}

	@Test
	public void recursivePopState() {
		runTest("java.lang.RecursivePopState");
		pathVerifier.totalPaths(1);
	}

	@Test
	public void multipleSinksInSameContext() {
		runTest("java.lang.MultipleSinksInSameContext");
		pathVerifier.startsAt(unitByLabel("@parameter0")).endsAt(unitByLabel("return")).times(2);
	}

	@Test
	@Ignore("not implemented yet")
	public void combiningMultipleTargets() {
		runTest("callersensitive.CombiningMultipleTargets");
		Assert.fail();
	}

	@Test
	@Ignore("not implemented yet")
	public void parameterWithTaintedField() {
		runTest("typestate.ParameterWithTaintedField");
		Assert.fail();
	}
}
