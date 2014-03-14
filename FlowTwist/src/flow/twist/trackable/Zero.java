package flow.twist.trackable;

import soot.Unit;

public class Zero extends Trackable {

	private Zero() {
		super();
	}

	public static final Trackable ZERO = new Zero();

	@Override
	public String toString() {
		return "ZERO";
	}

	@Override
	public Trackable createAlias(Unit sourceUnits) {
		throw new IllegalStateException("Never propagate ZERO, thus don't create any alias.");
	}

	@Override
	public Trackable cloneWithoutNeighborsAndPayload() {
		return ZERO;
	}

	@Override
	public int hashCode() {
		return 23543865;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
