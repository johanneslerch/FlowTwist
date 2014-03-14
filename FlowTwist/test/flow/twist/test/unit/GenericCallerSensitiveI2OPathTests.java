package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitByLabel;
import static flow.twist.test.util.selectors.UnitSelectorFactory.unitInMethod;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import flow.twist.SolverFactory;
import flow.twist.path.Path;
import flow.twist.reporter.CompositeReporter;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.test.util.AbstractPathTests;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.PathVerifier;
import flow.twist.transformer.StoreDataTransformer;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;

public class GenericCallerSensitiveI2OPathTests extends AbstractPathTests {

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
		CompositeReporter reporter = new CompositeReporter(verifier, new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage,
				new MergeEqualSelectorStrategy())));
		SolverFactory.runInnerToOuterSolver(i2oGenericCallerSensitiveDefaults().reporter(reporter));
		pathVerifier = new PathVerifier(dataStorage.getData());
		return verifier;
	}

	@Test
	public void callerSensitiveIntegrityAndConfidentiality() {
		runTest("callersensitive.IntegrityAndConfidentiality");
		pathVerifier.startsAt(unitInMethod("foo(", unitByLabel("@parameter0"))).endsAt(unitByLabel("return")).once();
	}

	@Test
	@Ignore("Current configuration filters sinks with one direction only")
	public void callerSensitiveIntegrity() {
		runTest("callersensitive.Integrity");
		pathVerifier.startsAt(unitInMethod("foo(", unitByLabel("dangerous"))).endsAt(unitByLabel("@parameter0")).once();
	}

	@Test
	@Ignore("Current configuration filters sinks with one direction only")
	public void callerSensitiveConfidentiality() {
		runTest("callersensitive.Confidentiality");
		pathVerifier.startsAt(unitInMethod("foo(", unitByLabel("dangerous"))).endsAt(unitByLabel("return")).once();
	}
}
