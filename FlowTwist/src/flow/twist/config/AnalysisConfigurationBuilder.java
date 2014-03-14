package flow.twist.config;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import flow.twist.TransitiveSinkCaller;
import flow.twist.config.AnalysisConfiguration.Type;
import flow.twist.debugger.Debugger;
import flow.twist.ifds.Propagator;
import flow.twist.ifds.PropagatorProvider;
import flow.twist.propagator.JavaUtilKiller;
import flow.twist.propagator.PrimitiveTaintKiller;
import flow.twist.propagator.SpecificMethodKiller;
import flow.twist.propagator.backwards.ArgumentSourceHandler;
import flow.twist.propagator.backwards.ReturnValuePropagator;
import flow.twist.propagator.backwards.SinkHandler;
import flow.twist.propagator.forwards.ClassInstantiationPropagator;
import flow.twist.propagator.forwards.I2OSourceRecognizer;
import flow.twist.propagator.forwards.I2OZeroHandler;
import flow.twist.propagator.forwards.PayloadSourceRecognizer;
import flow.twist.propagator.forwards.ZeroAtParameterHandler;
import flow.twist.reporter.IfdsReporter;
import flow.twist.targets.AnalysisTarget;
import flow.twist.targets.GenericCallerSensitive;
import flow.twist.targets.SimpleClassForNameTarget;
import flow.twist.util.Pair;

public class AnalysisConfigurationBuilder {

	private AnalysisConfiguration config;

	private AnalysisConfigurationBuilder() {
		config = new AnalysisConfiguration(new ArrayList<AnalysisTarget>(), null, null, null, null, null, null);
	}

	private AnalysisConfigurationBuilder(AnalysisConfiguration config) {
		this.config = new AnalysisConfiguration(config);
	}

	public AnalysisConfiguration build() {
		if (config.debugger == null)
			throw new IllegalStateException("Debugger not set.");
		if (config.direction == null)
			throw new IllegalStateException("Analysis direction not set.");
		if (config.propagatorProvider == null)
			throw new IllegalStateException("PropagatorProvider not set.");
		if (config.reporter == null)
			throw new IllegalStateException("PropagatorReporter not set.");
		if (config.seedFactory == null)
			throw new IllegalStateException("SeedFactory not set.");
		if (!config.targets.iterator().hasNext())
			throw new IllegalStateException("Not a single AnalysisTarget has been set.");
		if (config.type == null)
			throw new IllegalStateException("No Type of analysis is set.");
		return config;
	}

	public static AnalysisConfigurationBuilder i2oSimpleClassForNameDefaults() {
		//@formatter:off
		return new AnalysisConfigurationBuilder().targets(new SimpleClassForNameTarget())
				.type(Type.InnerToOuter)
				.seedFactory(new SeedFactory.I2OSeedFactory())
				.debugger(new Debugger())
				.propagator(new PropagatorProvider() {
					@Override
					public Propagator[][] provide(AnalysisContext context) {
						TransitiveSinkCaller transitiveSinkCaller = new TransitiveSinkCaller(context);
						if(context.direction == AnalysisDirection.FORWARDS) {
							return new Propagator[][] {
									{
										new PrimitiveTaintKiller(),
										new JavaUtilKiller(context),
										new SpecificMethodKiller(context),
									},
									{
//										new flow.twist.propagator.forwards.PropagateOverTarget(context)
									},
									{ 
										new I2OZeroHandler(context),
										new flow.twist.propagator.forwards.DefaultTaintPropagator(context), 
										new I2OSourceRecognizer(context, transitiveSinkCaller),
										new ClassInstantiationPropagator()
									} 
							};
						} else {
							return new Propagator[][] {
									{
										new PrimitiveTaintKiller(),
//										new NonStringKiller(),
										new JavaUtilKiller(context),
										new SpecificMethodKiller(context),
									},
									{ 
										new flow.twist.propagator.backwards.StringBuilderPropagator(), 
										new flow.twist.propagator.backwards.ShortcutPropagator(), 
										new flow.twist.propagator.backwards.PermissionCheckPropagator(),
//										new flow.twist.propagator.backwards.PropagateOverTarget(context),
									},
									{ 
										new SinkHandler(context),
										new ReturnValuePropagator(),
										new flow.twist.propagator.backwards.DefaultTaintPropagator(context), 
										new ArgumentSourceHandler(context, transitiveSinkCaller),
									} 
							};
						}
					}
				});
		// @formatter:on
	}

	public static AnalysisConfigurationBuilder i2oGenericCallerSensitiveDefaults() {
		//@formatter:off
		return new AnalysisConfigurationBuilder().targets(new GenericCallerSensitive())
				.type(Type.InnerToOuter)
				.seedFactory(new SeedFactory.I2OSeedFactory())
				.debugger(new Debugger())
				.propagator(new PropagatorProvider() {
					@Override
					public Propagator[][] provide(AnalysisContext context) {
						TransitiveSinkCaller transitiveSinkCaller = new TransitiveSinkCaller(context);
						if(context.direction == AnalysisDirection.FORWARDS) {
							return new Propagator[][] {
									{
										new PrimitiveTaintKiller(),
										new JavaUtilKiller(context),
										new SpecificMethodKiller(context),
									},
									{
										new flow.twist.propagator.forwards.PropagateOverTarget(context)
									},
									{ 
										new I2OZeroHandler(context),
										new flow.twist.propagator.forwards.DefaultTaintPropagator(context), 
										new I2OSourceRecognizer(context, transitiveSinkCaller),
//										new ClassInstantiationPropagator()
									} 
							};
						} else {
							return new Propagator[][] {
									{
										new PrimitiveTaintKiller(),
//										new NonStringKiller(),
										new JavaUtilKiller(context),
										new SpecificMethodKiller(context),
									},
									{ 
										new flow.twist.propagator.backwards.StringBuilderPropagator(), 
										new flow.twist.propagator.backwards.ShortcutPropagator(), 
										new flow.twist.propagator.backwards.PermissionCheckPropagator(),
										new flow.twist.propagator.backwards.PropagateOverTarget(context),
									},
									{ 
										new SinkHandler(context),
										new ReturnValuePropagator(),
										new flow.twist.propagator.backwards.DefaultTaintPropagator(context), 
										new ArgumentSourceHandler(context, transitiveSinkCaller),
									} 
							};
						}
					}
				});
		// @formatter:on
	}

	public static AnalysisConfigurationBuilder forwardsFromAllParametersDefaults(boolean genericCallerSensitive) {
		return forwardsFromParametersDefaults(genericCallerSensitive).type(Type.ForwardsFromAllParameters).direction(AnalysisDirection.FORWARDS)
				.seedFactory(new SeedFactory.AllParameterOfTransitiveCallersSeedFactory());
	}

	public static AnalysisConfigurationBuilder forwardsFromStringParametersDefaults(boolean genericCallerSensitive) {
		return forwardsFromParametersDefaults(genericCallerSensitive).type(Type.ForwardsFromStringParameters).direction(AnalysisDirection.FORWARDS)
				.seedFactory(new SeedFactory.StringParameterOfTransitiveCallersSeedFactory());
	}

	private static AnalysisConfigurationBuilder forwardsFromParametersDefaults(boolean genericCallerSensitive) {
		AnalysisConfigurationBuilder confBuilder = new AnalysisConfigurationBuilder().debugger(new Debugger());

		//@formatter:off
		if (genericCallerSensitive)
			return confBuilder.targets(new GenericCallerSensitive()).propagator(new PropagatorProvider() {
				@Override
				public Propagator[][] provide(AnalysisContext context) {
					return new Propagator[][] {
							{
								new PrimitiveTaintKiller(),
								new JavaUtilKiller(context),
							},
							{
								new flow.twist.propagator.forwards.StringBuilderPropagator(),
								new flow.twist.propagator.forwards.ShortcutPropagator(),
								new flow.twist.propagator.forwards.PermissionCheckPropagator(),
								new flow.twist.propagator.forwards.PropagateOverTarget(context),
							},
							{ 
								new ZeroAtParameterHandler(context),
								new flow.twist.propagator.forwards.DefaultTaintPropagator(context), 
								new PayloadSourceRecognizer(context),
							} 
					};
				}
			});
		else 
			return confBuilder.targets(new SimpleClassForNameTarget()).propagator(new PropagatorProvider() {
					@Override
					public Propagator[][] provide(AnalysisContext context) {
						return new Propagator[][] {
								{
									new PrimitiveTaintKiller(),
									new JavaUtilKiller(context),
								},
								{
									new flow.twist.propagator.forwards.StringBuilderPropagator(),
									new flow.twist.propagator.forwards.ShortcutPropagator(),
									new flow.twist.propagator.forwards.PermissionCheckPropagator(),
									new flow.twist.propagator.forwards.PropagateOverTarget(context),
								},
								{ 
									new ZeroAtParameterHandler(context),
									new flow.twist.propagator.forwards.DefaultTaintPropagator(context), 
									new PayloadSourceRecognizer(context),
									new ClassInstantiationPropagator()
								} 
						};
					}
				});
		// @formatter:on
	}

	public AnalysisConfigurationBuilder type(Type type) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, config.direction, type, config.reporter,
				config.seedFactory, config.debugger, config.propagatorProvider));
	}

	public AnalysisConfigurationBuilder seedFactory(SeedFactory seedFactory) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, config.direction, config.type, config.reporter,
				seedFactory, config.debugger, config.propagatorProvider));
	}

	public AnalysisConfigurationBuilder targets(AnalysisTarget... targets) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(Lists.newArrayList(targets), config.direction, config.type,
				config.reporter, config.seedFactory, config.debugger, config.propagatorProvider));
	}

	public AnalysisConfigurationBuilder direction(AnalysisDirection direction) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, direction, config.type, config.reporter,
				config.seedFactory, config.debugger, config.propagatorProvider));
	}

	public AnalysisConfigurationBuilder reporter(IfdsReporter reporter) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, config.direction, config.type, reporter,
				config.seedFactory, config.debugger, config.propagatorProvider));
	}

	public Pair<IfdsReporter, AnalysisConfigurationBuilder> decorateReporter(PropagationReporterDecorator decorator) {
		IfdsReporter reporter = decorator.decorate(config.reporter);
		return Pair.pair(reporter, reporter(reporter));
	}

	public AnalysisConfigurationBuilder debugger(Debugger debugger) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, config.direction, config.type, config.reporter,
				config.seedFactory, debugger, config.propagatorProvider));
	}

	public AnalysisConfigurationBuilder propagator(PropagatorProvider propagatorProvider) {
		return new AnalysisConfigurationBuilder(new AnalysisConfiguration(config.targets, config.direction, config.type, config.reporter,
				config.seedFactory, config.debugger, propagatorProvider));
	}

	@Override
	public String toString() {
		return config.toString();
	}

	public static interface PropagationReporterDecorator {
		IfdsReporter decorate(IfdsReporter reporter);
	}
}
