package flow.twist.pathchecker;

import java.util.Set;

import flow.twist.path.Path;
import flow.twist.targets.AnalysisTarget;

public class EmptyCallStackChecker implements PathChecker {

	private PathChecker decoratee;

	public EmptyCallStackChecker(PathChecker decoratee) {
		this.decoratee = decoratee;
	}

	@Override
	public void addOrDismiss(Path path) {
		if (path.getCallStack().isEmpty())
			decoratee.addOrDismiss(path);
	}

	@Override
	public Set<Path> getValidPaths() {
		return decoratee.getValidPaths();
	}

	@Override
	public AnalysisTarget getTarget() {
		return decoratee.getTarget();
	}

}
