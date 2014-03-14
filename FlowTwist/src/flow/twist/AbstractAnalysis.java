package flow.twist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;

public abstract class AbstractAnalysis {

	public void execute() {
		ArrayList<String> argList = createArgs();
		AnalysisReporting.setSootArgs(argList);
		registerAnalysisTransformer();
		soot.Main.main(argList.toArray(new String[0]));
	}

	protected abstract ArrayList<String> createArgs();

	protected abstract void executeAnalysis();

	private static void insertNopStatements() {
		for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
			SootMethod m = iter.next().method();
			if (m.hasActiveBody()) {
				Body b = m.getActiveBody();
				NopStmt newNopStmt = Jimple.v().newNopStmt();
				newNopStmt.addAllTagsOf(b.getUnits().getFirst());
				b.getUnits().addFirst(newNopStmt);

				ActiveBodyVerifier.markActive(m);
			} else
				ActiveBodyVerifier.markInactive(m);
		}
	}

	private void registerAnalysisTransformer() {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.permissionAnalysis", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Stats.print();
				insertNopStatements();
				executeAnalysis();
			}
		}));
	}
}
