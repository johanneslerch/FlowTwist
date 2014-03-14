package flow.twist.test.util;

import static java.lang.String.format;
import heros.solver.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.Assert;

import soot.SootMethod;
import soot.Unit;
import soot.util.IdentityHashSet;

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.reporter.IfdsReporter;
import flow.twist.reporter.Report;
import flow.twist.reporter.TrackableGraphPlotter;
import flow.twist.test.util.selectors.TrackableSelector;
import flow.twist.test.util.selectors.UnitSelector;
import flow.twist.trackable.Trackable;

public class AnalysisGraphVerifier implements IfdsReporter {

	private TrackableGraphPlotter debugPlotter;
	private Map<Unit, SootMethod> unit2Method = Maps.newHashMap();
	private Multimap<Unit, Pair<Unit, Trackable>> successors = HashMultimap.create();
	private Multimap<Unit, Trackable> trackablesAtUnit = Multimaps.newSetMultimap(new HashMap<Unit, Collection<Trackable>>(),
			new Supplier<Set<Trackable>>() {
				@Override
				public Set<Trackable> get() {
					return new IdentityHashSet<>();
				}
			});

	public AnalysisGraphVerifier() {
		// debugPlotter = new DebugPlotter();
		debugPlotter = new TrackableGraphPlotter();
	}

	@Override
	public void reportTrackable(Report report) {
		debugPlotter.reportTrackable(report);
		createEdges(report.context, report.trackable, report.targetUnit);
	}

	private void createEdges(AnalysisContext context, Trackable trackable, Unit unit) {
		unit2Method.put(unit, context.icfg.getMethodOf(unit));

		for (Trackable neighbor : trackable.getSelfAndNeighbors()) {
			trackablesAtUnit.put(unit, neighbor);
			if (neighbor.sourceUnit == null)
				continue;

			if (successors.put(neighbor.sourceUnit, new Pair<Unit, Trackable>(unit, neighbor)))
				createEdges(context, neighbor.predecessor, neighbor.sourceUnit);
		}
	}

	@Override
	public void analysisFinished() {
	}

	public void writeDebugFile(String fileName) {
		System.out.println("Writing debug graph file for failed test: " + fileName);
		debugPlotter.writeFile(fileName);
	}

	public UnitNode find(UnitSelector selector) {
		for (Unit unit : unit2Method.keySet()) {
			if (selector.matches(unit2Method.get(unit), unit)) {
				return new UnitNode(unit);
			}
		}
		throw new AssertionFailedError(format("Unit not found: %s", selector));
	}

	public void cannotFind(UnitSelector selector) {
		for (Unit unit : unit2Method.keySet()) {
			if (selector.matches(unit2Method.get(unit), unit)) {
				throw new AssertionFailedError(format("Unit found: %s", selector));
			}
		}
	}

	public class UnitNode {

		private final Unit unit;

		public UnitNode(Unit unit) {
			this.unit = unit;
		}

		public UnitNode edge(TrackableSelector trackableSelector, UnitSelector unitSelector) {
			for (Pair<Unit, Trackable> successor : successors.get(unit)) {
				if (unitSelector.matches(unit2Method.get(successor.getO1()), successor.getO1()) && trackableSelector.matches(successor.getO2())) {
					return new UnitNode(successor.getO1());
				}
			}

			throw new AssertionFailedError(format("No edge found from '%s' to '%s' with trackable matching '%s'", unit.toString(), unitSelector,
					trackableSelector.toString()));
		}

		public UnitNode pathTo(TrackableSelector trackableSelector, UnitSelector selector) {
			Set<Unit> visited = Sets.newHashSet();
			List<Unit> worklist = Lists.newLinkedList();
			worklist.add(unit);
			while (!worklist.isEmpty()) {
				Unit current = worklist.remove(0);
				if (visited.add(current)) {
					for (Pair<Unit, Trackable> successor : successors.get(current)) {
						if (selector.matches(unit2Method.get(successor.getO1()), successor.getO1()) && trackableSelector.matches(successor.getO2())) {
							return new UnitNode(successor.getO1());
						}
						worklist.add(successor.getO1());
					}
				}
			}
			throw new AssertionFailedError(format("There is no connection from '%s' to '%s' with the taint '%s' at the destination.",
					unit.toString(), selector, trackableSelector.toString()));
		}

		public void assertHasNot(TrackableSelector trackableSelector) {
			for (Pair<Unit, Trackable> successor : successors.get(unit)) {
				if (trackableSelector.matches(successor.getO2())) {
					throw new AssertionFailedError(format("There is a taint matching '%s' into unit '%s' that should not be there!",
							trackableSelector, unit));
				}
			}
		}

		public void assertNoOutgoingEdges() {
			Assert.assertEquals(format("Unit '%s' has outgoing edges, but should have none.", unit), 0, successors.get(unit).size());
		}

		public void assertIncomingEdges(int expected) {
			int actual = 0;
			for (Trackable t : trackablesAtUnit.get(unit)) {
				if (t.predecessor != null)
					actual++;
			}
			Assert.assertEquals(expected, actual);
		}

	}

}
