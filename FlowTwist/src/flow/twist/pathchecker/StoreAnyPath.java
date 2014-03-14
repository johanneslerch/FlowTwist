package flow.twist.pathchecker;

import java.util.Set;

import com.google.common.collect.Sets;

import flow.twist.path.Path;
import flow.twist.targets.AnalysisTarget;

public class StoreAnyPath implements PathChecker {

	private Set<Path> paths = Sets.newHashSet();
	private AnalysisTarget analysisTarget;

	public StoreAnyPath(AnalysisTarget analysisTarget) {
		this.analysisTarget = analysisTarget;
	}

	@Override
	public void addOrDismiss(Path path) {
		paths.add(path);
	}

	@Override
	public Set<Path> getValidPaths() {
		return paths;
	}

	@Override
	public AnalysisTarget getTarget() {
		return analysisTarget;
	}
}
