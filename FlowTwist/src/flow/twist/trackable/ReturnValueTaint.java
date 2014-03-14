package flow.twist.trackable;

import soot.Type;
import soot.Unit;

public class ReturnValueTaint extends Trackable implements PopFromStack {

	public final Type type;

	public ReturnValueTaint(Unit sourceUnit, Trackable predecessor, Type type) {
		super(sourceUnit, predecessor);
		this.type = type;
	}

	@Override
	public String toString() {
		return "ReturnValue" + payloadString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ReturnValueTaint))
			return false;
		ReturnValueTaint other = (ReturnValueTaint) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public Trackable createAlias(Unit sourceUnit) {
		throw new IllegalStateException("Never propagate ReturnValueTaint, thus don't create any alias.");
	}

	@Override
	public ReturnValueTaint cloneWithoutNeighborsAndPayload() {
		return new ReturnValueTaint(sourceUnit, predecessor, type);
	}

	@Override
	public Unit getCallSite() {
		return sourceUnit;
	}
}
