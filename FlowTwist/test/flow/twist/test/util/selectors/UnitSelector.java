package flow.twist.test.util.selectors;

import soot.SootMethod;
import soot.Unit;

public interface UnitSelector {
	boolean matches(SootMethod method, Unit unit);
}