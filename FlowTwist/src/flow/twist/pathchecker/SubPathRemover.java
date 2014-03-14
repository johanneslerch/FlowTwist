package flow.twist.pathchecker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import flow.twist.path.Path;
import flow.twist.targets.AnalysisTarget;

public class SubPathRemover implements PathChecker {

	private PathChecker decoratee;

	public SubPathRemover(PathChecker decoratee) {
		this.decoratee = decoratee;
	}

	@Override
	public void addOrDismiss(Path path) {
		decoratee.addOrDismiss(path);
	}

	@Override
	public Set<Path> getValidPaths() {
		Set<Path> paths = new HashSet<Path>(decoratee.getValidPaths());
		Iterator<Path> iterator = paths.iterator();
		outer: while (iterator.hasNext()) {
			Path p = iterator.next();

			for (Path other : paths) {
				if (p == other)
					continue;

				if (p.context == other.context && p.isSubPath(other)) {
					iterator.remove();
					continue outer;
				}
			}
		}

		return paths;
	}

	@Override
	public AnalysisTarget getTarget() {
		return decoratee.getTarget();
	}

}
