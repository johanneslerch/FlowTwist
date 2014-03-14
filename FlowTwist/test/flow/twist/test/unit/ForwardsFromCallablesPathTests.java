package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.forwardsFromAllParametersDefaults;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;

import java.util.Set;

import org.junit.Test;

import flow.twist.SolverFactory;
import flow.twist.debugger.ClassNameFilter;
import flow.twist.debugger.Debugger;
import flow.twist.debugger.ShortConsoleDebugger;
import flow.twist.path.Path;
import flow.twist.reporter.CompositeReporter;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.test.util.AbstractPathTests;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.PathVerifier;
import flow.twist.transformer.StoreDataTransformer;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;

public class ForwardsFromCallablesPathTests extends AbstractPathTests {

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
		CompositeReporter reporter = new CompositeReporter(verifier, new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage,
				new MergeEqualSelectorStrategy())));
		Debugger debugger = new Debugger();
		debugger.addFilter(new ClassNameFilter(classNames));
		debugger.registerListener(new ShortConsoleDebugger());
		SolverFactory.runOneDirectionSolver(forwardsFromAllParametersDefaults(true).reporter(reporter).debugger(debugger));
		pathVerifier = new PathVerifier(dataStorage.getData());
		return verifier;
	}

	@Test
	public void recursion() {
		// for each sink two equal paths are generated because
		// of summary functions built for ZERO and the parameter
		runTest("java.lang.Recursion");
		pathVerifier.totalPaths(6);

		pathVerifier.startsAt(unitInMethod("recursive(", unitByLabel("@parameter0"))).endsAt(unitInMethod("recursive(", unitByLabel("return")))
				.times(3);
		pathVerifier.startsAt(unitInMethod("recursiveA(", unitByLabel("@parameter0"))).endsAt(unitInMethod("recursiveA(", unitByLabel("return")))
				.times(3);
	}

	@Test
	public void recursionAndClassHierarchy() {
		// for each entry method two summaries are generated. One for the ZERO
		// case and one where the Parameter is tainted. The latter ones
		// disappear in the reported paths, due to recursion elimination in path
		// generation. Nevertheless, they result in equal taint paths reported
		// two times.
		runTest("java.lang.RecursionAndClassHierarchy", "java.lang.RecursionAndClassHierarchy$BaseInterface",
				"java.lang.RecursionAndClassHierarchy$SubClassA", "java.lang.RecursionAndClassHierarchy$SubClassB",
				"java.lang.RecursionAndClassHierarchy$SubClassC");

		pathVerifier.totalPaths(8);
	}

	@Test
	public void combiningMultipleTargets() {
		runTest("callersensitive.CombiningMultipleTargets");
		pathVerifier.totalPaths(1);
	}
}
