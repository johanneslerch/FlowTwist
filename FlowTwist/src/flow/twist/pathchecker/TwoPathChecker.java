package flow.twist.pathchecker;

import java.util.LinkedList;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.config.AnalysisDirection;
import flow.twist.path.Path;
import flow.twist.targets.AnalysisTarget;

public class TwoPathChecker implements PathChecker {

	private PathChecker decoratee;

	public TwoPathChecker(PathChecker decoratee) {
		this.decoratee = decoratee;
	}

	@Override
	public void addOrDismiss(Path path) {
		decoratee.addOrDismiss(path);
	}

	@Override
	public Set<Path> getValidPaths() {
		Set<Path> result = Sets.newHashSet();
		HashMultimap<SimilarPathIndication, Path> similarPaths = HashMultimap.create();
		for (Path path : decoratee.getValidPaths()) {
			similarPaths.put(new SimilarPathIndication(path), path);
		}

		for (SimilarPathIndication key : similarPaths.keySet()) {
			Set<Path> paths = similarPaths.get(key);
			Set<Path> forwards = Sets.newHashSet();
			Set<Path> backwards = Sets.newHashSet();

			for (Path path : paths) {
				if (path.context.direction == AnalysisDirection.BACKWARDS)
					backwards.add(path);
				else
					forwards.add(path);
			}

			for (Path backwardPath : backwards) {
				for (Path forwardPath : forwards) {
					result.add(backwardPath.reverse().append(forwardPath));
				}
			}
			// if (result.isEmpty()) {
			// for (Path path : paths) {
			// System.out.println("Dismissing: " + path);
			// }
			// }
		}

		return result;
	}

	private static class SimilarPathIndication {

		private final Unit sink;
		private final SootMethod source;
		private final LinkedList<Object> callStack;

		public SimilarPathIndication(Path path) {
			sink = path.getFirst();
			source = path.context.icfg.getMethodOf(path.getFirst());
			callStack = Lists.newLinkedList(path.getCallStack());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((callStack == null) ? 0 : callStack.hashCode());
			result = prime * result + ((sink == null) ? 0 : sink.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SimilarPathIndication other = (SimilarPathIndication) obj;
			if (callStack == null) {
				if (other.callStack != null)
					return false;
			} else if (!callStack.equals(other.callStack))
				return false;
			if (sink == null) {
				if (other.sink != null)
					return false;
			} else if (!sink.equals(other.sink))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}
	}

	@Override
	public AnalysisTarget getTarget() {
		return decoratee.getTarget();
	}

}
