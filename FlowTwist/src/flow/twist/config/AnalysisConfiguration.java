package flow.twist.config;

import flow.twist.debugger.Debugger;
import flow.twist.ifds.PropagatorProvider;
import flow.twist.reporter.IfdsReporter;
import flow.twist.targets.AnalysisTarget;

public class AnalysisConfiguration {

	public final Iterable<AnalysisTarget> targets;
	public final AnalysisDirection direction;
	public final IfdsReporter reporter;
	public final Type type;
	public final Debugger debugger;
	public final SeedFactory seedFactory;
	public final PropagatorProvider propagatorProvider;

	public AnalysisConfiguration(Iterable<AnalysisTarget> targets, AnalysisDirection direction, Type type, IfdsReporter reporter,
			SeedFactory seedFactory, Debugger debugger, PropagatorProvider propagatorProvider) {
		this.targets = targets;
		this.direction = direction;
		this.reporter = reporter;
		this.type = type;
		this.debugger = debugger;
		this.seedFactory = seedFactory;
		this.propagatorProvider = propagatorProvider;
	}

	public AnalysisConfiguration(AnalysisConfiguration config) {
		this.targets = config.targets;
		this.direction = config.direction;
		this.reporter = config.reporter;
		this.type = config.type;
		this.debugger = config.debugger;
		this.seedFactory = config.seedFactory;
		this.propagatorProvider = config.propagatorProvider;
	}

	public static enum Type {
		InnerToOuter, ForwardsFromAllParameters, ForwardsFromStringParameters
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Direction: ");
		builder.append(direction);
		builder.append("\n");

		builder.append("Type: ");
		builder.append(type);
		builder.append("\n");

		builder.append("Seed Factory: ");
		builder.append(seedFactory == null ? "" : seedFactory.getClass().getSimpleName());
		builder.append("\n");

		builder.append("Analysis Targets:\n");
		for (AnalysisTarget target : targets) {
			builder.append("\t");
			builder.append(target.getClass().getSimpleName());
			builder.append("\n");
		}

		builder.append("Reporter:\n\t");
		builder.append(reporter.toString());

		return builder.toString();
	}
}
