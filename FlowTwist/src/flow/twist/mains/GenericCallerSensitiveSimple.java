package flow.twist.mains;

import java.util.Set;

import flow.twist.AbstractMainAnalysis;
import flow.twist.SolverFactory;
import flow.twist.config.AnalysisConfigurationBuilder;
import flow.twist.path.Path;
import flow.twist.pathchecker.FilterSingleDirectionReports;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.transformer.StoreDataTransformer;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;

public class GenericCallerSensitiveSimple {

	public static void main(final String[] args) {
		new AbstractMainAnalysis(args) {
			@Override
			protected Set<Path> _executeAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults().reporter(
						new ResultForwardingReporter(new FilterSingleDirectionReports(new PathBuilderResultTransformer(dataStorage,
								new MergeEqualSelectorStrategy())))));
				return dataStorage.getData();
			}
		}.execute();
	}
}
