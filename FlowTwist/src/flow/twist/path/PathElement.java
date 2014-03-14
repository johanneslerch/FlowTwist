package flow.twist.path;

import soot.Unit;
import flow.twist.trackable.Trackable;

public class PathElement implements Comparable<PathElement> {

	public final Unit from;
	public final Trackable trackable;
	public final Unit to;

	public PathElement(Unit from, Trackable trackable, Unit to) {
		this.from = from;
		this.trackable = trackable;
		this.to = to;
	}

	@Override
	public String toString() {
		return String.format("%s (%d) -----%s-----> %s (%d)", from, from.hashCode(), trackable, to, to.hashCode());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((trackable == null) ? 0 : System.identityHashCode(trackable));
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
		PathElement other = (PathElement) obj;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (trackable != other.trackable)
			return false;
		return true;
	}

	public PathElement reverse() {
		return new PathElement(to, trackable, from);
	}

	@Override
	public int compareTo(PathElement o) {
		return Integer.compare(hashCode(), o.hashCode());
	}

}
