package flow.twist.trackable;

import soot.Type;
import soot.Unit;
import soot.Value;

public class ReturnEdgeTaint extends Taint implements PushOnStack {

	public final Unit callSite;
	public final int paramIndex;

	public ReturnEdgeTaint(Unit callSite, Unit exitStmt, Trackable predecessor, Value value, int paramIndex, Type type) {
		super(exitStmt, predecessor, value, type);
		this.callSite = callSite;
		this.paramIndex = paramIndex;
	}

	@Override
	public String toString() {
		return "[ReturnEdgeTaint" + payloadString() + ": " + value + "]";
	}

	@Override
	public Unit getCallSite() {
		return callSite;
	}

	@Override
	public int getParameterIndex() {
		return paramIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReturnEdgeTaint other = (ReturnEdgeTaint) obj;
		if (callSite == null) {
			if (other.callSite != null)
				return false;
		} else if (!callSite.equals(other.callSite))
			return false;
		return true;
	}

}
