package flow.twist.ifds;

import flow.twist.config.AnalysisContext;
import flow.twist.ifds.FlowEdge.Call2ReturnEdge;
import flow.twist.ifds.FlowEdge.CallEdge;
import flow.twist.ifds.FlowEdge.NormalEdge;
import flow.twist.ifds.FlowEdge.ReturnEdge;
import flow.twist.ifds.FlowEdge.Visitor;
import flow.twist.ifds.Propagator.KillGenInfo;
import flow.twist.trackable.Trackable;
import heros.FlowFunction;
import heros.FlowFunctions;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class FlowFunctionFactory implements FlowFunctions<Unit, Trackable, SootMethod> {

	public static AtomicInteger propCounter = new AtomicInteger(0);
	private Propagator[][] propagators;
	private AnalysisContext context;

	public FlowFunctionFactory(AnalysisContext context) {
		this.context = context;
		propagators = context.propagatorProvider.provide(context);
	}

	private Iterable<Propagator> matchingPropagators(final Trackable taint, Propagator[] phase) {
		return Iterables.filter(Arrays.asList(phase), new Predicate<Propagator>() {
			@Override
			public boolean apply(Propagator propagator) {
				return propagator.canHandle(taint);
			}
		});
	}

	private FlowFunction<Trackable> propagate(final FlowEdge edge) {
		propCounter.incrementAndGet();
		return new FlowFunction<Trackable>() {
			@Override
			public Set<Trackable> computeTargets(final Trackable source) {
				final Set<Trackable> propagatedTaints = Sets.newHashSet();
				boolean killed = false;

				for (Propagator[] phase : propagators) {
					for (Propagator propagator : matchingPropagators(source, phase)) {
						PropagatorVisitor visitor = new PropagatorVisitor(propagator, source);
						KillGenInfo killGen = edge.accept(visitor);
						propagatedTaints.addAll(killGen.gen());
						killed |= killGen.kill;

						if (killGen.kill) {
							context.debugger.kill(context, source, propagator, edge);
						}
					}
					if (killed)
						break;
				}

				if (!killed) {
					// identity propagations

					// propagatedTaints.add(source);
					propagatedTaints.add(edge.accept(new Visitor<Trackable>() {
						@Override
						public Trackable visit(Call2ReturnEdge call2ReturnEdge) {
							// TODO: unclear if necessary to create an alias
							return source.createAlias(call2ReturnEdge.callSite);
							// return source;
						}

						@Override
						public Trackable visit(ReturnEdge returnEdge) {
							// identity doesn't make any sense
							throw new IllegalStateException();
						}

						@Override
						public Trackable visit(CallEdge callEdge) {
							// identity doesn't make any sense
							throw new IllegalStateException();
						}

						@Override
						public Trackable visit(NormalEdge normalEdge) {
							// TODO: Here a real difference occurs. Not sure,
							// why.
							return source.createAlias(normalEdge.curr);
							// return source;
						}
					}));
				}

				if (!propagatedTaints.isEmpty())
					context.debugger.propagate(context, source, propagatedTaints, edge);
				return propagatedTaints;
			}
		};
	}

	@Override
	public FlowFunction<Trackable> getNormalFlowFunction(final Unit curr, final Unit succ) {
		return propagate(new NormalEdge(curr, succ));
	}

	@Override
	public FlowFunction<Trackable> getCallFlowFunction(final Unit callStmt, final SootMethod destinationMethod) {
		return propagate(new FlowEdge.CallEdge(callStmt, destinationMethod));
	}

	@Override
	public FlowFunction<Trackable> getReturnFlowFunction(final Unit callSite, final SootMethod calleeMethod,
			final Unit exitStmt, final Unit returnSite) {
		return propagate(new FlowEdge.ReturnEdge(callSite, calleeMethod, exitStmt, returnSite));
	}

	@Override
	public FlowFunction<Trackable> getCallToReturnFlowFunction(final Unit callSite, final Unit returnSite) {
		return propagate(new FlowEdge.Call2ReturnEdge((Stmt) callSite, returnSite));
	}

	private static class PropagatorVisitor implements FlowEdge.Visitor<KillGenInfo> {

		private Propagator propagator;
		private Trackable source;

		public PropagatorVisitor(Propagator propagator, Trackable source) {
			this.propagator = propagator;
			this.source = source;
		}

		@Override
		public KillGenInfo visit(NormalEdge normalEdge) {
			return propagator.propagateNormalFlow(source, normalEdge.curr, normalEdge.succ);
		}

		@Override
		public KillGenInfo visit(CallEdge callEdge) {
			return propagator.propagateCallFlow(source, callEdge.callStmt, callEdge.destinationMethod);
		}

		@Override
		public KillGenInfo visit(ReturnEdge returnEdge) {
			return propagator.propagateReturnFlow(source, returnEdge.callSite, returnEdge.calleeMethod,
					returnEdge.exitStmt, returnEdge.returnSite);
		}

		@Override
		public KillGenInfo visit(Call2ReturnEdge call2ReturnEdge) {
			return propagator.propagateCallToReturnFlow(source, call2ReturnEdge.callSite);
		}

	}
}
