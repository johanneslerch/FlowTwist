package flow.twist.test.util;

import java.util.List;
import java.util.Set;

import org.junit.Assert;

import soot.Unit;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.test.util.selectors.UnitSelector;

public class PathVerifier {

	private Set<Path> paths;

	public PathVerifier(Set<Path> paths) {
		this.paths = paths;
	}

	public void totalPaths(int numberOfExpectedPaths) {
		Assert.assertEquals(numberOfExpectedPaths, paths.size());
	}

	public Set<Path> getPaths() {
		return paths;
	}

	public PathSelector startsAt(UnitSelector selector) {
		List<Path> matchingPaths = Lists.newLinkedList();
		for (Path path : paths) {
			Unit unit = path.getFirst();
			if (selector.matches(path.context.icfg.getMethodOf(unit), unit)) {
				matchingPaths.add(path);
			}
		}
		return new PathSelector(matchingPaths);
	}

	public class PathSelector {
		private List<Path> paths;

		public PathSelector(List<Path> paths) {
			this.paths = paths;
		}

		public PathSelector endsAt(UnitSelector selector) {
			List<Path> matchingPaths = Lists.newLinkedList();
			for (Path path : paths) {
				Unit unit = path.getLast();
				if (selector.matches(path.context.icfg.getMethodOf(unit), unit)) {
					matchingPaths.add(path);
				}
			}
			return new PathSelector(matchingPaths);
		}

		public PathSelector contains(UnitSelector selector) {
			List<Path> matchingPaths = Lists.newLinkedList();
			for (Path path : paths) {
				if (pathContains(selector, path)) {
					matchingPaths.add(path);
				}
			}
			return new PathSelector(matchingPaths);
		}

		private boolean pathContains(UnitSelector selector, Path path) {
			for (PathElement element : path) {
				if (selector.matches(path.context.icfg.getMethodOf(element.to), element.to)) {
					return true;
				}
			}

			return false;
		}

		public PathSelector doesNotContain(UnitSelector selector) {
			List<Path> matchingPaths = Lists.newLinkedList();
			for (Path path : paths) {
				if (!pathContains(selector, path)) {
					matchingPaths.add(path);
				}
			}
			return new PathSelector(matchingPaths);
		}

		public void once() {
			Assert.assertEquals(1, paths.size());
		}

		public void never() {
			Assert.assertEquals(0, paths.size());
		}

		public void noOtherPaths() {
			Assert.assertEquals(paths.size(), PathVerifier.this.paths.size());
		}

		public void times(int i) {
			Assert.assertEquals(i, paths.size());
		}

		public void assertStack(Unit... callSites) {
			for (Path path : paths) {
				fj.data.List<Object> callStack = path.getCallStack();
				Assert.assertTrue(Iterables.elementsEqual(callStack, Lists.newArrayList(callSites)));
			}
		}

	}

}
