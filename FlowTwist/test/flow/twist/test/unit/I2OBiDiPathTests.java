package flow.twist.test.unit;

import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;

import java.util.Set;

import flow.twist.SolverFactory;
import flow.twist.path.Path;
import flow.twist.pathchecker.FilterSingleDirectionReports;
import flow.twist.reporter.CompositeReporter;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.test.util.AbstractPathTests;
import flow.twist.test.util.AnalysisGraphVerifier;
import flow.twist.test.util.PathVerifier;
import flow.twist.transformer.StoreDataTransformer;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;

public class I2OBiDiPathTests extends AbstractPathTests {

	@Override
	protected AnalysisGraphVerifier executeTaintAnalysis(String[] classNames) {
		AnalysisGraphVerifier verifier = new AnalysisGraphVerifier();
		StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
		CompositeReporter reporter = new CompositeReporter(verifier, new ResultForwardingReporter(new FilterSingleDirectionReports(
				new PathBuilderResultTransformer(dataStorage, new MergeEqualSelectorStrategy()))));
		SolverFactory.runBiDirectionSolver(i2oSimpleClassForNameDefaults().reporter(reporter));
		pathVerifier = new PathVerifier(dataStorage.getData());
		return verifier;
	}
}
