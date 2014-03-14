package flow.twist.transformer.path;

import static com.google.common.collect.Sets.newHashSet;
import static flow.twist.trackable.Zero.ZERO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.trackable.PopFromStack;
import flow.twist.trackable.PushOnStack;
import flow.twist.trackable.Trackable;

public class MergeIntraproceduralSelectorStrategy extends FirstIntroductionSelectorStrategy {

	@Override
	protected Iterable<Trackable> firstIntroductionOf(Collection<Trackable> selfAndNeighbors) {
		Set<SourceDependentTrackableWrapper> result = newHashSet();
		Set<Trackable> visited = Sets.newIdentityHashSet();
		List<Trackable> worklist = Lists.newLinkedList(selfAndNeighbors);
		while (!worklist.isEmpty()) {
			Trackable current = worklist.remove(0);

			if (current == ZERO || current instanceof PushOnStack || current instanceof PopFromStack) {
				result.add(new SourceDependentTrackableWrapper(current));
			} else {
				for (Trackable t : current.predecessor.getSelfAndNeighbors()) {
					if (visited.add(t)) {
						if (t == ZERO)
							result.add(new SourceDependentTrackableWrapper(current));
						else
							worklist.add(t);
					}
				}
			}
		}

		return Iterables.transform(result, new Function<SourceDependentTrackableWrapper, Trackable>() {
			@Override
			public Trackable apply(SourceDependentTrackableWrapper wrapper) {
				return wrapper.trackable;
			}
		});
	}

	private static class SourceDependentTrackableWrapper {

		private Trackable trackable;

		private SourceDependentTrackableWrapper(Trackable trackable) {
			this.trackable = trackable;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((trackable == null) ? 0 : trackable.hashCode() + (trackable.predecessor == null ? 0 : trackable.predecessor.hashCode()));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SourceDependentTrackableWrapper))
				return false;
			SourceDependentTrackableWrapper other = (SourceDependentTrackableWrapper) obj;
			if (trackable == null) {
				if (other.trackable != null)
					return false;
			} else if (!trackable.equals(other.trackable) || trackable.sourceUnit != other.trackable.sourceUnit
					|| trackable.predecessor != other.trackable.predecessor)
				return false;
			return true;
		}
	}

}
