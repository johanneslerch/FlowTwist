package flow.twist.test.util;

import soot.Unit;
import flow.twist.trackable.PopFromStack;
import flow.twist.trackable.Trackable;

public class TestTaintPopFromStack extends TestTaint implements PopFromStack {

	public TestTaintPopFromStack(Unit callSite, Trackable predecessor, String identifier) {
		super(callSite, predecessor, identifier);
	}

	@Override
	public Unit getCallSite() {
		return sourceUnit;
	}

}
