package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import flow.twist.SolverFactory;
import flow.twist.reporter.CompositeReporter;
import flow.twist.states.StateMachineBasedPathReporter;
import flow.twist.states.StatePlotter;
import flow.twist.test.util.AbstractPathTests;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.PathVerifier;

public class StateBasedPathTests extends AbstractPathTests {

	private StatePlotter statePlotter;

	@Rule
	public TestWatcher watcher = new TestWatcher() {
		@Override
		public void failed(Throwable e, Description description) {
			StringBuilder builder = new StringBuilder();
			builder.append("tmp/");
			builder.append(StateBasedPathTests.this.getClass().getSimpleName());
			builder.append("_");
			builder.append(description.getMethodName());
			statePlotter.write(builder.toString() + "_states");
		}
	};

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		StateMachineBasedPathReporter vulnerablePathReporter = new StateMachineBasedPathReporter();
		CompositeReporter reporter = new CompositeReporter(verifier, vulnerablePathReporter);
		SolverFactory.runInnerToOuterSolver(i2oSimpleClassForNameDefaults().reporter(reporter));
		// SolverFactory.runBiDirectionSolver(reporter);
		pathVerifier = new PathVerifier(vulnerablePathReporter.getValidPaths());
		statePlotter = new StatePlotter(vulnerablePathReporter);
		return verifier;
	}

	@Test
	public void decoratingClassHierarchyWithDifferentBehaviors() {
		runTest("java.lang.DecoratingClassHierarchyWithDifferentBehaviors", "java.lang.DecoratingClassHierarchyWithDifferentBehaviors$BaseInterface",
				"java.lang.DecoratingClassHierarchyWithDifferentBehaviors$SubClassA",
				"java.lang.DecoratingClassHierarchyWithDifferentBehaviors$SubClassB",
				"java.lang.DecoratingClassHierarchyWithDifferentBehaviors$SubClassC");

		pathVerifier.totalPaths(2);
		pathVerifier.startsAt(unitInMethod("SubClassC", unitByLabel("@parameter0"))).endsAt(unitInMethod("SubClassC", unitByLabel("return"))).once();
		pathVerifier.startsAt(unitInMethod("SubClassA", unitByLabel("@parameter1"))).endsAt(unitInMethod("SubClassA", unitByLabel("return"))).once();
	}

	@Override
	@Ignore
	public void recursion() {
	}

	@Override
	@Ignore
	public void recursivePopState() {
	}
}
