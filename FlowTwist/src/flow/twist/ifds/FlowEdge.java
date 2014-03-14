package flow.twist.ifds;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public abstract class FlowEdge {

	public abstract <T> T accept(Visitor<T> visitor);

	public static interface Visitor<T> {

		T visit(NormalEdge normalEdge);

		T visit(CallEdge callEdge);

		T visit(ReturnEdge returnEdge);

		T visit(Call2ReturnEdge call2ReturnEdge);
	}

	public static class NormalEdge extends FlowEdge {

		public final Unit curr;
		public final Unit succ;

		public NormalEdge(Unit curr, Unit succ) {
			this.curr = curr;
			this.succ = succ;
		}

		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "[normal] " + curr.toString();
		}
	}

	public static class CallEdge extends FlowEdge {

		public final Unit callStmt;
		public final SootMethod destinationMethod;

		public CallEdge(Unit callStmt, SootMethod destinationMethod) {
			this.callStmt = callStmt;
			this.destinationMethod = destinationMethod;
		}

		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "[call] " + callStmt + " - " + destinationMethod;
		}
	}

	public static class ReturnEdge extends FlowEdge {

		public final Unit callSite;
		public final SootMethod calleeMethod;
		public final Unit exitStmt;
		public final Unit returnSite;

		public ReturnEdge(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
			this.callSite = callSite;
			this.calleeMethod = calleeMethod;
			this.exitStmt = exitStmt;
			this.returnSite = returnSite;
		}

		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "[return] " + returnSite + " (" + callSite + ")";
		}
	}

	public static class Call2ReturnEdge extends FlowEdge {

		public final Stmt callSite;
		public final Unit returnSite;

		public Call2ReturnEdge(Stmt callSite, Unit returnSite) {
			this.callSite = callSite;
			this.returnSite = returnSite;
		}

		@Override
		public <T> T accept(Visitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "[call2ret] " + callSite;
		}
	}
}
