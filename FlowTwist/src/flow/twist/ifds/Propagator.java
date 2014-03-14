package flow.twist.ifds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public interface Propagator {

	boolean canHandle(Trackable trackable);

	KillGenInfo propagateNormalFlow(Trackable trackable, Unit curr, Unit succ);

	KillGenInfo propagateCallFlow(Trackable trackable, Unit callStmt, SootMethod destinationMethod);

	KillGenInfo propagateReturnFlow(Trackable trackable, Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite);

	KillGenInfo propagateCallToReturnFlow(Trackable trackable, Stmt callSite);

	public static class KillGenInfo {

		protected final Collection<? extends Trackable> gen;
		public final boolean kill;

		private static final KillGenInfo ID = new KillGenInfo(false, Collections.<Trackable> emptySet());

		public KillGenInfo(boolean kill, Collection<? extends Trackable> gen) {
			this.kill = kill;
			this.gen = gen;

		}

		public Collection<? extends Trackable> gen() {
			return gen;
		}

		public static KillGenInfo kill() {
			return new KillGenInfo(true, Collections.<Trackable> emptySet());
		}

		public static KillGenInfo gen(Trackable... gen) {
			if (gen.length == 0)
				throw new IllegalArgumentException();
			return new KillGenInfo(false, Arrays.asList(gen));
		}

		public static KillGenInfo propagate(Trackable... gen) {
			if (gen.length == 0)
				throw new IllegalArgumentException();
			return new KillGenInfo(true, Arrays.asList(gen));
		}

		public static KillGenInfo identity() {
			return ID;
		}
	}

}
