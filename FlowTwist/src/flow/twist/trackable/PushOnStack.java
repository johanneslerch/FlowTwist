package flow.twist.trackable;

import soot.Unit;

public interface PushOnStack {

	public Unit getCallSite();

	public int getParameterIndex();
}
