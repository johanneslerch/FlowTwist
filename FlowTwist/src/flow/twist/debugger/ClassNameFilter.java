package flow.twist.debugger;

import soot.Unit;
import flow.twist.config.AnalysisContext;
import flow.twist.ifds.FlowEdge;
import flow.twist.ifds.FlowEdge.Call2ReturnEdge;
import flow.twist.ifds.FlowEdge.CallEdge;
import flow.twist.ifds.FlowEdge.NormalEdge;
import flow.twist.ifds.FlowEdge.ReturnEdge;
import flow.twist.ifds.FlowEdge.Visitor;

public class ClassNameFilter implements Debugger.Filter {

	private String[] classNames;

	public ClassNameFilter(String[] classNames) {
		this.classNames = classNames;
	}

	private boolean matchesClassName(AnalysisContext context, Unit unit) {
		String candidateClassName = context.icfg.getMethodOf(unit).getDeclaringClass().getName();
		for (String className : classNames) {
			if (className.equals(candidateClassName))
				return true;
		}
		return false;
	}

	@Override
	public boolean filter(final AnalysisContext context, FlowEdge edge) {
		return edge.accept(new Visitor<Boolean>() {
			@Override
			public Boolean visit(NormalEdge normalEdge) {
				return matchesClassName(context, normalEdge.curr);
			}

			@Override
			public Boolean visit(CallEdge callEdge) {
				return matchesClassName(context, callEdge.callStmt);
			}

			@Override
			public Boolean visit(ReturnEdge returnEdge) {
				return matchesClassName(context, returnEdge.exitStmt);
			}

			@Override
			public Boolean visit(Call2ReturnEdge call2ReturnEdge) {
				return matchesClassName(context, call2ReturnEdge.callSite);
			}

		});
	}

}
