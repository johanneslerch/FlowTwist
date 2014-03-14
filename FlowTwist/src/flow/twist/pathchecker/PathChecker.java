package flow.twist.pathchecker;

import java.util.Set;

import flow.twist.path.Path;
import flow.twist.targets.AnalysisTarget;

public interface PathChecker {

	void addOrDismiss(Path path);

	Set<Path> getValidPaths();

	AnalysisTarget getTarget();
}
