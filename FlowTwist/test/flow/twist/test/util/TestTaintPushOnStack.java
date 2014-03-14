package flow.twist.test.util;

import soot.Unit;
import flow.twist.trackable.PushOnStack;
import flow.twist.trackable.Trackable;

public class TestTaintPushOnStack extends TestTaint implements PushOnStack {

	private Unit callSite;

	public TestTaintPushOnStack(Unit sourceUnit, Trackable predecessor, Unit callSite, String identifier) {
		super(sourceUnit, predecessor, identifier);
		this.callSite = callSite;
	}

	public TestTaintPushOnStack(Trackable predecessor, Unit callSite, String identifier) {
		super(predecessor, identifier);
		this.callSite = callSite;
	}

	@Override
	public Unit getCallSite() {
		return callSite;
	}

	@Override
	public int getParameterIndex() {
		return 0;
	}

}
