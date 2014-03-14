package flow.twist.config;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import flow.twist.ifds.Propagator;

public class AnalysisContext extends AnalysisConfiguration {

	public final BiDiInterproceduralCFG<Unit, SootMethod> icfg;

	public AnalysisContext(AnalysisConfiguration config, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		super(config);
		this.icfg = icfg;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString());
		builder.append("\n");

		builder.append("Propagators:\n");
		Propagator[][] propagators = propagatorProvider.provide(this);
		for (Propagator[] outer : propagators) {
			builder.append("\t{\n");
			for (Propagator inner : outer) {
				builder.append("\t\tnew ");
				builder.append(inner.getClass().getName());
				builder.append(",\n");
			}
			builder.append("\t},\n");
		}

		return builder.toString();
	}
}
