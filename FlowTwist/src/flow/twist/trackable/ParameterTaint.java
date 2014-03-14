package flow.twist.trackable;

import soot.Type;
import soot.Unit;
import soot.Value;

public class ParameterTaint extends Taint implements PopFromStack {

	public ParameterTaint(Unit sourceUnit, Trackable predecessor, Value value, Type type) {
		super(sourceUnit, predecessor, value, type);
	}

	@Override
	public String toString() {
		return "[ParameterTaint" + payloadString() + ": " + value + "]";
	}

	@Override
	public Unit getCallSite() {
		return sourceUnit;
	}
}
