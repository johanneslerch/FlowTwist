package flow.twist;

import static flow.twist.config.AnalysisDirection.BACKWARDS;
import static flow.twist.config.AnalysisDirection.FORWARDS;
import heros.solver.BiDiIFDSSolver;
import heros.solver.IFDSSolver;
import heros.solver.PathTrackingIFDSSolver;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import flow.twist.config.AnalysisConfigurationBuilder;
import flow.twist.config.AnalysisConfigurationBuilder.PropagationReporterDecorator;
import flow.twist.ifds.FlowFunctionFactory;
import flow.twist.ifds.TabulationProblem;
import flow.twist.reporter.DelayingReporter;
import flow.twist.reporter.IfdsReporter;
import flow.twist.trackable.Trackable;
import flow.twist.util.Pair;

public class SolverFactory {

	public static void runOneDirectionSolver(AnalysisConfigurationBuilder configBuilder) {

		Pair<IfdsReporter, AnalysisConfigurationBuilder> pair = configBuilder.decorateReporter(new PropagationReporterDecorator() {
			@Override
			public IfdsReporter decorate(IfdsReporter reporter) {
				return new DelayingReporter(reporter);
			}
		});
		
		TabulationProblem problem = createTabulationProblem(pair.second);
		IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> solver = createSolver(problem);

		AnalysisReporting.analysisStarted();
		
		_runOneDirectionSolver(problem, solver);
		pair.first.analysisFinished();
		AnalysisReporting.analysisFinished();
	}

	public static void runInnerToOuterSolver(AnalysisConfigurationBuilder configBuilder) {

		Pair<IfdsReporter, AnalysisConfigurationBuilder> pair = configBuilder.decorateReporter(new PropagationReporterDecorator() {
			@Override
			public IfdsReporter decorate(IfdsReporter reporter) {
				return new DelayingReporter(reporter);
			}
		});
		configBuilder = pair.second;
		
		TabulationProblem forwardTabulationProblem = createTabulationProblem(configBuilder.direction(FORWARDS));
		IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> forwardSolver = createSolver(forwardTabulationProblem);
		TabulationProblem backwardTabulationProblem = createTabulationProblem(configBuilder.direction(BACKWARDS));
		IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> backwardSolver = createSolver(backwardTabulationProblem);

		AnalysisReporting.analysisStarted();
		
		_runOneDirectionSolver(forwardTabulationProblem, forwardSolver);
		_runOneDirectionSolver(backwardTabulationProblem, backwardSolver);
		pair.first.analysisFinished();
		AnalysisReporting.analysisFinished();
	}

	protected static void _runOneDirectionSolver(final TabulationProblem tabulationProblem, IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> solver) {
		AnalysisReporting.ifdsStarting(tabulationProblem.getContext(), solver);
		solver.solve();
		AnalysisReporting.ifdsFinished(tabulationProblem.getContext(), solver, FlowFunctionFactory.propCounter.get());
		FlowFunctionFactory.propCounter.set(0);
	}

	private static IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> createSolver(
			final TabulationProblem tabulationProblem) {
		IFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> solver = new PathTrackingIFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>>(
				tabulationProblem) {
			@Override
			protected String getDebugName() {
				if (tabulationProblem.getContext().direction == FORWARDS)
					return "FW";
				else
					return "BW";
			}
		};
		return solver;
	}

	private static TabulationProblem createTabulationProblem(
			AnalysisConfigurationBuilder configBuilder) {
		final TabulationProblem tabulationProblem = new TabulationProblem(configBuilder.build());
		return tabulationProblem;
	}

	public static void runBiDirectionSolver(AnalysisConfigurationBuilder configBuilder) {
		Pair<IfdsReporter, AnalysisConfigurationBuilder> pair = configBuilder.decorateReporter(new PropagationReporterDecorator() {
			@Override
			public IfdsReporter decorate(IfdsReporter reporter) {
				return new DelayingReporter(reporter);
			}
		});
		configBuilder = pair.second;

		TabulationProblem backwardsTabulationProblem = new TabulationProblem(configBuilder.direction(BACKWARDS).build());
		TabulationProblem forwardsTabulationProblem = new TabulationProblem(configBuilder.direction(FORWARDS).build());

		BiDiIFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> biDiIFDSSolver = new BiDiIFDSSolver<Unit, Trackable, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>>(
				forwardsTabulationProblem, backwardsTabulationProblem);

		AnalysisReporting.analysisStarted();
		
		AnalysisReporting.ifdsStarting(backwardsTabulationProblem.getContext(), biDiIFDSSolver);
		AnalysisReporting.ifdsStarting(forwardsTabulationProblem.getContext(), biDiIFDSSolver);
		biDiIFDSSolver.solve();
		AnalysisReporting.ifdsFinished(backwardsTabulationProblem.getContext(), biDiIFDSSolver, FlowFunctionFactory.propCounter.get());
		AnalysisReporting.ifdsFinished(forwardsTabulationProblem.getContext(), biDiIFDSSolver, FlowFunctionFactory.propCounter.get());
		pair.first.analysisFinished();
		AnalysisReporting.analysisFinished();
	}
}
