package flow.twist.test.util.selectors;

import flow.twist.trackable.Trackable;

public interface TrackableSelector {
	boolean matches(Trackable trackable);
}