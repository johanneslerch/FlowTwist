package flow.twist.test.unit.transformer.path;

import static com.google.common.collect.Sets.newHashSet;
import static flow.twist.test.util.TestTaint.setEqual;
import static flow.twist.test.util.selectors.UnitSelectorFactory.specificUnit;
import static flow.twist.trackable.Zero.ZERO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import flow.twist.config.AnalysisConfiguration;
import flow.twist.config.AnalysisContext;
import flow.twist.path.Path;
import flow.twist.reporter.Report;
import flow.twist.reporter.TrackableGraphPlotter;
import flow.twist.targets.AnalysisTarget;
import flow.twist.test.util.PathVerifier;
import flow.twist.test.util.PathVerifier.PathSelector;
import flow.twist.test.util.TestTaint;
import flow.twist.test.util.TestTaintPopFromStack;
import flow.twist.test.util.TestTaintPushOnStack;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilder;

public class PathBuilderTest {

	private PathBuilder sut;
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private AnalysisContext context;
	private Unit targetUnit;
	private Multimap<Unit, SootMethod> callSiteToDeclarationMapping;
	private TrackableGraphPlotter plotter;

	@Rule
	public TestWatcher watcher = new TestWatcher() {
		@Override
		public void failed(Throwable e, Description description) {
			StringBuilder builder = new StringBuilder();
			builder.append("tmp/");
			builder.append(PathBuilderTest.this.getClass().getSimpleName());
			builder.append("_");
			builder.append(description.getMethodName());

			plotter.writeFile(builder.toString());
		}
	};

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		plotter = new TrackableGraphPlotter();
		sut = new PathBuilder(new MergeEqualSelectorStrategy()) {
			@Override
			protected Set<SootMethod> getInitialDeclaration(Unit callSite) {
				return newHashSet(callSiteToDeclarationMapping.get(callSite));
			}
		};
		callSiteToDeclarationMapping = HashMultimap.create();
		icfg = mock(BiDiInterproceduralCFG.class);
		context = new AnalysisContext(new AnalysisConfiguration(Lists.<AnalysisTarget> newLinkedList(), null, null,
				null, null, null, null), icfg);
		targetUnit = mock(Unit.class);
		when(targetUnit.toString()).thenReturn("targetUnit");
	}

	@Test
	public void trivialIntraproceduralPath() {
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");

		PathVerifier verifier = pathVerifierForReportAt(a2);
		verifier.totalPaths(1);
		verifier.startsAt(specificUnit(a1.sourceUnit)).contains(specificUnit(a2.sourceUnit))
				.endsAt(specificUnit(targetUnit)).times(1);
	}

	@Test
	public void trivialInterproceduralPath() {
		Unit callSite = mock(Unit.class);
		TestTaint a1 = new TestTaintPushOnStack(ZERO, callSite, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		PathVerifier verifier = pathVerifierForReportAt(a2);
		verifier.totalPaths(1);
		PathSelector selector = verifier.startsAt(specificUnit(a1.sourceUnit)).contains(specificUnit(a2.sourceUnit))
				.endsAt(specificUnit(targetUnit));
		selector.times(1);
		selector.assertStack(callSite);
	}

	@Test
	public void invalidCallStack() {
		TestTaint a1 = new TestTaintPopFromStack(mock(Unit.class), ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		PathVerifier verifier = pathVerifierForReportAt(a2);
		verifier.totalPaths(0);
	}

	@Test
	public void loop() {
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint b1 = new TestTaint(a2, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		a1.addNeighbor(b3);

		PathVerifier verifier = pathVerifierForReportAt(a3);
		verifier.totalPaths(1);
		verifier.startsAt(specificUnit(a1.sourceUnit)).doesNotContain(specificUnit(b2.sourceUnit))
				.endsAt(specificUnit(targetUnit)).once();
	}

	@Test
	public void branchAndJoin() {
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint b1 = new TestTaint(a1.sourceUnit, ZERO, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		setEqual(a1, a2, b1, b2);
		a2.addNeighbor(b2);

		PathVerifier verifier = pathVerifierForReportAt(a2);
		verifier.totalPaths(1);
		verifier.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
	}

	@Test
	public void callNotOnStack() {
		Unit callSite = mock(Unit.class);
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaintPopFromStack(callSite, a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint a4 = new TestTaintPushOnStack(a3, callSite, "a4");

		PathVerifier verifier = pathVerifierForReportAt(a4);
		verifier.totalPaths(1);
		PathSelector selector = verifier.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
		selector.once();
		selector.assertStack();
	}

	@Test
	public void testRecursiveHierarchy() {
		SootMethod initDeclMethod = mock(SootMethod.class);

		Unit callSiteA = mock(Unit.class);
		when(callSiteA.toString()).thenReturn("CallSiteA");
		Unit callSiteD = mock(Unit.class);
		when(callSiteD.toString()).thenReturn("CallSiteD");

		callSiteToDeclarationMapping.put(callSiteA, initDeclMethod);
		callSiteToDeclarationMapping.put(callSiteD, initDeclMethod);

		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaintPushOnStack(a1, callSiteA, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");

		TestTaint b1 = new TestTaintPushOnStack(a3, callSiteA, "b1");
		a2.addNeighbor(b1);

		TestTaint c1 = new TestTaintPushOnStack(b1.sourceUnit, a3, callSiteD, "c1");

		TestTaint d1 = new TestTaintPushOnStack(a2.sourceUnit, a1, callSiteD, "d1");
		c1.addNeighbor(d1);
		TestTaint d2 = new TestTaint(c1, "d2");

		TestTaint e1 = new TestTaintPushOnStack(d2, callSiteD, "e1");
		c1.addNeighbor(e1);

		TestTaint f1 = new TestTaintPushOnStack(e1.sourceUnit, d2, callSiteA, "f1");
		a2.addNeighbor(f1);

		PathVerifier verifierA = pathVerifierForReportAt(a3);
		verifierA.totalPaths(1);
		PathSelector selectorA = verifierA.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
		selectorA.once();
		selectorA.assertStack(callSiteA);

		PathVerifier verifierD = pathVerifierForReportAt(d2);
		verifierD.totalPaths(1);
		PathSelector selectorD = verifierD.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
		selectorD.once();
		selectorD.assertStack(callSiteD);
	}

	@Test
	public void multipleCallsToDifferentMethods() {
		Unit callSite1 = mock(Unit.class);
		when(callSite1.toString()).thenReturn("CallSite1");
		Unit callSite2 = mock(Unit.class);
		when(callSite2.toString()).thenReturn("CallSite2");

		callSiteToDeclarationMapping.put(callSite1, mock(SootMethod.class));
		callSiteToDeclarationMapping.put(callSite2, mock(SootMethod.class));

		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaintPopFromStack(callSite1, a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint a4 = new TestTaintPushOnStack(a3, callSite1, "a4");
		TestTaint a5 = new TestTaint(a4, "a5");
		TestTaint a6 = new TestTaintPopFromStack(callSite2, a5, "a6");
		a2.addNeighbor(a6);
		TestTaint a7 = new TestTaintPushOnStack(a4.sourceUnit, a3, callSite2, "a7");
		TestTaint a8 = new TestTaint(a7, "a8");

		PathVerifier verifier = pathVerifierForReportAt(a8);
		verifier.totalPaths(1);
		PathSelector selector = verifier.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
		selector.once();
		selector.assertStack();
	}

	@Test
	public void multipleCallsToSameMethod() {
		SootMethod initDeclMethod = mock(SootMethod.class);

		Unit callSite1 = mock(Unit.class);
		when(callSite1.toString()).thenReturn("CallSite1");
		Unit callSite2 = mock(Unit.class);
		when(callSite2.toString()).thenReturn("CallSite2");

		callSiteToDeclarationMapping.put(callSite1, initDeclMethod);
		callSiteToDeclarationMapping.put(callSite2, initDeclMethod);

		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaintPopFromStack(callSite1, a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint a4 = new TestTaintPushOnStack(a3, callSite1, "a4");
		TestTaint a5 = new TestTaint(a4, "a5");
		TestTaint a6 = new TestTaintPopFromStack(callSite2, a5, "a6");
		a2.addNeighbor(a6);
		TestTaint a7 = new TestTaintPushOnStack(a4.sourceUnit, a3, callSite2, "a7");
		TestTaint a8 = new TestTaint(a7, "a8");

		PathVerifier verifier = pathVerifierForReportAt(a8);
		verifier.totalPaths(1);
		PathSelector selector = verifier.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
		selector.once();
		selector.assertStack();
	}

	@Test
	public void joinAtReport() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint b1 = new TestTaint(a1.sourceUnit, Zero.ZERO, "b1");
		TestTaint b2 = new TestTaint(a2.sourceUnit, b1, "b2");
		a2.addNeighbor(b2);
		setEqual(a1, a2, b1, b2);

		PathVerifier verifierA = pathVerifierForReportAt(a2);
		verifierA.totalPaths(1);
		verifierA.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit)).once();
	}

	@Test
	public void branchAtSink() {
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");

		TestTaint b1 = new TestTaint(a1.sourceUnit, ZERO, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		a1.addNeighbor(b3);
		setEqual(a1, a2, a3, b1, b2, b3);

		PathVerifier verifier = pathVerifierForReportAt(a3);
		verifier.totalPaths(1);
		verifier.startsAt(specificUnit(a1.sourceUnit)).endsAt(specificUnit(targetUnit));
	}

	@Test
	public void multipleSinks() {
		TestTaint a1 = new TestTaint(ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint b1 = new TestTaint(ZERO, "b1");
		TestTaint b2 = new TestTaint(a2.sourceUnit, b1, "b2");
		TestTaint b3 = new TestTaint(a3.sourceUnit, b2, "b3");

		a3.addNeighbor(b3);
		setEqual(a1, a2, b1, b2);
		setEqual(a3, b3);

		PathVerifier verifier = pathVerifierForReportAt(a3);
		verifier.totalPaths(2);
		verifier.startsAt(specificUnit(a1.sourceUnit)).contains(specificUnit(a3.sourceUnit))
				.endsAt(specificUnit(targetUnit)).once();
		verifier.startsAt(specificUnit(b1.sourceUnit)).contains(specificUnit(b3.sourceUnit))
				.endsAt(specificUnit(targetUnit)).once();
	}

	private PathVerifier pathVerifierForReportAt(Trackable t) {
		Report report = new Report(context, t, targetUnit);
		Set<Path> paths = sut.createPaths(report);

		plotter.reportTrackable(new Report(context, t, targetUnit));
		return new PathVerifier(paths);
	}
}
