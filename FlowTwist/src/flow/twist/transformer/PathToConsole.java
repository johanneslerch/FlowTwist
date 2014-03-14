package flow.twist.transformer;

import java.util.Set;

import flow.twist.path.Path;

public class PathToConsole extends ResultTransformer<Set<Path>, Void> {

	public PathToConsole() {
		super(null);
	}

	@Override
	protected Void transformAnalysisResults(Set<Path> from) {
		for (Path path : from) {
			System.out.println(path.toContextAwareString());
			System.out.println();
		}
		return null;
	}

}
