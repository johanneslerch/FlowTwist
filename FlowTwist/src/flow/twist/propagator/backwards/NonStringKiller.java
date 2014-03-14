package flow.twist.propagator.backwards;

import static flow.twist.ifds.Propagator.KillGenInfo.identity;
import static flow.twist.ifds.Propagator.KillGenInfo.kill;
import soot.ArrayType;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Stmt;
import flow.twist.ifds.Propagator;
import flow.twist.trackable.Taint;
import flow.twist.trackable.Trackable;

public class NonStringKiller implements Propagator {

	@Override
	public boolean canHandle(Trackable trackable) {
		return trackable instanceof Taint;
	}

	public static boolean isWrongType(Value value) {
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

		if (!type.toString().contains("String") && !type.toString().contains("Object") && !type.toString().contains("Serializable")
				&& !type.toString().contains("Comparable") && !type.toString().contains("CharSequence"))
			return true;

		return false;
	}

	@Override
	public KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ) {
		if (isWrongType(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod) {
		if (isWrongType(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
		if (isWrongType(((Taint) trackable).value))
			return kill();
		return identity();
	}

	@Override
	public KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite) {
		if (isWrongType(((Taint) trackable).value))
			return kill();
		return identity();
	}

}
