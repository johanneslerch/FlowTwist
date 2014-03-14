package flow.twist.propagator;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public class SpecificMethodKiller implements Propagator {

	private AnalysisContext context;

	public SpecificMethodKiller(AnalysisContext context) {
		this.context = context;
	}

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof Taint;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		if (destinationMethod.toString().contains("sun.org.mozilla.javascript.internal.Interpreter"))
			return kill();

		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (returnSite != null && context.icfg.getMethodOf(returnSite).toString().contains("sun.org.mozilla.javascript.internal.Interpreter"))
			return kill();

		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		return identity();
	}

}
