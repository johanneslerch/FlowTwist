package flow.twist.test.util.selectors;

import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public class TrackableSelectorFactory {

	public static TrackableSelector taintContains(final String name) {
		return new TrackableSelector() {
	
			@Override
			public boolean matches(Trackable trackable) {
				if (trackable instanceof Taint) {
					String taintString = ((Taint) trackable).value.toString();
					if (taintString.contains(name)) {
						return true;
					}
	
				}
				return false;
			}
	
			@Override
			public String toString() {
				return ".*" + name + ".*";
			}
		};
	}

	public static TrackableSelector taint(final String name) {
		return new TrackableSelector() {
	
			@Override
			public boolean matches(Trackable trackable) {
				if (trackable instanceof Taint) {
					String taintString = ((Taint) trackable).value.toString();
					if (taintString.equals(name)) {
						return true;
					}
				}
				return false;
			}
	
			@Override
			public String toString() {
				return name;
			}
		};
	}

	public static TrackableSelector anyTaint() {
		return new TrackableSelector() {
	
			@Override
			public boolean matches(Trackable trackable) {
				return true;
			}
	
			@Override
			public String toString() {
				return "any Taint";
			}
		};
	}

}
