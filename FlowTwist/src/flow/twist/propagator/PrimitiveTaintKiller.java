package flow.twist.propagator;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import flow.twist.ifds.Propagator;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;
import soot.ArrayType;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Stmt;

public class PrimitiveTaintKiller implements Propagator {

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof Taint;
	}

	private boolean isPrimitive(Value value) {
		while (value.getType() instanceof ArrayRef) {
			value = ((ArrayRef) value.getType()).getBase();
		}
		if (value instanceof PrimType) {
			return true;
		}

		Type type = value.getType();
		while (type instanceof ArrayType) {
			type = ((ArrayType) type).getArrayElementType();
		}
		if (type instanceof PrimType)
			return true;

		return false;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		if (isPrimitive(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		if (isPrimitive(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (isPrimitive(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (isPrimitive(((Taint) trackable).value))
			return kill();
		return identity();
	}

}
