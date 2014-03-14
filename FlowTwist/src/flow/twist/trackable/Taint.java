package flow.twist.trackable;

import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;

public class Taint extends Trackable {

	public final Value value;
	public final Type type;

	public Taint(Unit sourceUnit, Trackable predecessor, Value value, Type type) {
		super(sourceUnit, predecessor);

		if (value instanceof Constant) {
			throw new IllegalArgumentException("Constant value has been tainted.");
		}

		this.value = value;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Taint))
			return false;
		Taint other = (Taint) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "[Taint" + payloadString() + ": " + value + " Type: " + type + "]";
		// return "[Taint: " + value + "; " + System.identityHashCode(this) +
		// "; " + System.identityHashCode(value) + "; " + hashCode() + "]";
	}

	public Taint createAlias(Value value, Unit sourceUnit) {
		return new Taint(sourceUnit, this, value, type);
	}

	@Override
	public Trackable createAlias(Unit sourceUnit) {
		return new Taint(sourceUnit, this, value, type);
	}

	@Override
	public Taint cloneWithoutNeighborsAndPayload() {
		return new Taint(sourceUnit, predecessor, value, type);
	}

}
