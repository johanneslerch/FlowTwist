package flow.twist.transformer.path;

import java.util.Collection;

import flow.twist.trackable.Trackable;

public abstract class FirstIntroductionSelectorStrategy {

	public Iterable<Trackable> firstIntroductionOf(Trackable trackable) {
		return firstIntroductionOf(trackable.getSelfAndNeighbors());
	}

	protected abstract Iterable<Trackable> firstIntroductionOf(Collection<Trackable> selfAndNeighbors);
}
