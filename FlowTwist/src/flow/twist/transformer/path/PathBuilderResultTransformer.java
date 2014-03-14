package flow.twist.transformer.path;

import java.util.Set;

import com.google.common.collect.Sets;

import fj.data.Option;
import flow.twist.AnalysisReporting;
import flow.twist.config.AnalysisContext;
import flow.twist.path.Path;
import flow.twist.pathchecker.PathChecker;
import flow.twist.reporter.Report;
import flow.twist.targets.AnalysisTarget;
import flow.twist.transformer.ResultTransformer;

public class PathBuilderResultTransformer extends ResultTransformer<Iterable<Report>, Set<Path>> {

	private FirstIntroductionSelectorStrategy introductionStrategy;
	private PathBuilder pathBuilder;

	public PathBuilderResultTransformer(ResultTransformer<Set<Path>, ?> successor, FirstIntroductionSelectorStrategy introductionStrategy) {
		super(successor);
		this.introductionStrategy = introductionStrategy;
		pathBuilder = new PathBuilder(introductionStrategy);
	}

	@Override
	protected Set<Path> transformAnalysisResults(Iterable<Report> from) {
		Set<Path> validPaths = Sets.newHashSet();
		Set<PathChecker> pathCheckersInUse = Sets.newHashSet();

		for (Report report : from) {
			AnalysisReporting.startingPathsFor(report);
			Set<Path> paths = pathBuilder.createPaths(report);
			if (paths.size() == 0) {
				System.err.println("0 paths created. BUG!?");
			}
			for (Path path : paths) {
				pathCheckersInUse.addAll(addToMatchingPathChecker(report.context, path));
			}
			AnalysisReporting.finishedPathsFor(report, paths);
		}

		AnalysisReporting.combinedPathsStarting();
		for (PathChecker checker : pathCheckersInUse) {
			validPaths.addAll(checker.getValidPaths());
		}
		AnalysisReporting.combinedPathsFinished(validPaths);
		return validPaths;
	}

	private Set<PathChecker> addToMatchingPathChecker(AnalysisContext context, Path path) {
		Set<PathChecker> pathCheckersInUse = Sets.newHashSet();
		for (AnalysisTarget target : context.targets) {
			Option<PathChecker> pathChecker = target.getPathChecker(path);
			if (pathChecker.isSome()) {
				pathCheckersInUse.add(pathChecker.some());
				pathChecker.some().addOrDismiss(path);
			}
		}
		return pathCheckersInUse;
	}

	@Override
	protected String debugInformation() {
		return introductionStrategy.getClass().getSimpleName();
	}
}
