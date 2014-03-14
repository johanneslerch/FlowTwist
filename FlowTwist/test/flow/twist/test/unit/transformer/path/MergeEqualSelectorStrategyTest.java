package flow.twist.test.unit.transformer.path;

import static com.google.common.collect.Iterables.size;
import static flow.twist.test.util.TestTaint.setEqual;
import static flow.twist.trackable.Zero.ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.config.AnalysisConfiguration;
import flow.twist.config.AnalysisContext;
import flow.twist.reporter.Report;
import flow.twist.reporter.TrackableGraphPlotter;
import flow.twist.targets.AnalysisTarget;
import flow.twist.test.util.TestTaint;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;

public class MergeEqualSelectorStrategyTest {

	private MergeEqualSelectorStrategy sut = new MergeEqualSelectorStrategy();
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private AnalysisContext context;

	@Rule
	public TestWatcher watcher = new TestWatcher() {
		@Override
		public void failed(Throwable e, Description description) {
			StringBuilder builder = new StringBuilder();
			builder.append("tmp/");
			builder.append(MergeEqualSelectorStrategyTest.this.getClass().getSimpleName());
			builder.append("_");
			builder.append(description.getMethodName());

			plotter.writeFile(builder.toString());
		}
	};

	TrackableGraphPlotter plotter;

	@Before
	public void setup() {
		plotter = new TrackableGraphPlotter();
		icfg = mock(BiDiInterproceduralCFG.class);
		context = new AnalysisContext(new AnalysisConfiguration(Lists.<AnalysisTarget> newLinkedList(), null, null,
				null, null, null, null), icfg);
	}

	private Iterable<Trackable> exercise(Trackable t) {
		Iterable<Trackable> result = sut.firstIntroductionOf(t);
		plotter.reportTrackable(new Report(context, t, null));
		return result;
	}

	@Test
	public void identity() {
		TestTaint a = new TestTaint(Zero.ZERO, "a");
		TestTaint b = new TestTaint(a, "b");
		assertElementsIdentical(newIdentitySet(b), exercise(b));
		assertElementsIdentical(newIdentitySet(a), exercise(a));
	}

	@Test
	public void joinTaintChain1() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(Zero.ZERO, "a3");
		a3.addNeighbor(a2);
		TestTaint.setEqual(a1, a2, a3);
		// assertElementsIdentical(newIdentitySet(a1, a3), exercise(a2));
		assertElementsIdentical(newIdentitySet(a1, a3), exercise(a3));
	}

	@Test
	public void branchTaintChain() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint a4 = new TestTaint(a2.sourceUnit, a1, "a4");
		TestTaint.setEqual(a1, a2, a3, a4);
		assertElementsIdentical(newIdentitySet(a1), exercise(a3));
		assertElementsIdentical(newIdentitySet(a1), exercise(a4));
	}

	@Test
	public void joinTaintChain2() {
		TestTaint a = new TestTaint(Zero.ZERO, "a");
		TestTaint d = new TestTaint(Zero.ZERO, "d");
		TestTaint b1 = new TestTaint(a, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		TestTaint c1 = new TestTaint(d, "c1");
		TestTaint c2 = new TestTaint(c1, "c2");
		TestTaint c3 = new TestTaint(c2, "c3");
		TestTaint.setEqual(b1, b2, b3, c1, c2, c3);
		b3.addNeighbor(c3);
		assertElementsIdentical(newIdentitySet(b1, c1), exercise(b3));
		// assertElementsIdentical(newIdentitySet(b1, c1), exercise(c3));
	}

	@Test
	public void branchAndJoinTaintChain() {
		TestTaint a = new TestTaint(Zero.ZERO, "a");
		TestTaint b1 = new TestTaint(a, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		TestTaint c1 = new TestTaint(b1.sourceUnit, a, "c1");
		TestTaint c2 = new TestTaint(c1, "c2");
		TestTaint c3 = new TestTaint(c2, "c3");
		TestTaint.setEqual(b1, b2, b3, c1, c2, c3);
		b3.addNeighbor(c3);

		Iterable<Trackable> actualForB3 = exercise(b3);
		assertEquals(1, size(actualForB3));
		assertHasEqualTrackableWithSameSourceUnit(b1, actualForB3);

		Iterable<Trackable> actualForC3 = exercise(c3);
		assertEquals(1, size(actualForC3));
		assertHasEqualTrackableWithSameSourceUnit(b1, actualForC3);
	}

	@Test
	public void branchAndJoinEqualTaintChain() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint b1 = new TestTaint(a2, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		TestTaint c1 = new TestTaint(b1.sourceUnit, a2, "c1");
		TestTaint c2 = new TestTaint(c1, "c2");
		TestTaint c3 = new TestTaint(c2, "c3");
		TestTaint.setEqual(a1, a2, b1, b2, b3, c1, c2, c3);
		b3.addNeighbor(c3);
		assertElementsIdentical(newIdentitySet(a1), exercise(b3));
		assertElementsIdentical(newIdentitySet(a1), exercise(c3));
	}

	@Test
	public void loop() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint a3 = new TestTaint(a2, "a3");
		TestTaint b1 = new TestTaint(a3.sourceUnit, a2, "b1");
		TestTaint b2 = new TestTaint(b1, "b2");
		TestTaint b3 = new TestTaint(b2, "b3");
		TestTaint.setEqual(a1, a2, a3, b1, b2, b3);
		a1.addNeighbor(b3);
		assertElementsIdentical(newIdentitySet(a1), exercise(a3));
	}

	@Test
	public void joinAtReport() {
		TestTaint a1 = new TestTaint(Zero.ZERO, "a1");
		TestTaint a2 = new TestTaint(a1, "a2");
		TestTaint b1 = new TestTaint(a1.sourceUnit, Zero.ZERO, "b1");
		TestTaint b2 = new TestTaint(a2.sourceUnit, b1, "b2");
		a2.addNeighbor(b2);
		setEqual(a1, a2, b1, b2);

		Iterable<Trackable> actualForA2 = exercise(a2);
		assertEquals(1, size(actualForA2));
		assertHasEqualTrackableWithSameSourceUnit(a1, actualForA2);

		Iterable<Trackable> actualForB2 = exercise(b2);
		assertEquals(1, size(actualForB2));
		assertHasEqualTrackableWithSameSourceUnit(b1, actualForB2);
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
		assertElementsIdentical(newIdentitySet(a1), exercise(a3));
	}

	private static <T> void assertElementsIdentical(Iterable<? extends T> expected, Iterable<? extends T> actual) {
		assertEquals(size(expected), size(actual));
		for (T exp : expected) {
			boolean found = false;
			for (T act : actual) {
				if (exp == act) {
					found = true;
					break;
				}
			}
			if (!found) {
				fail("Expected: " + exp + "; actual: " + Joiner.on(",").join(actual).toString());
			}
		}
	}

	private static void assertHasEqualTrackableWithSameSourceUnit(TestTaint expected, Iterable<Trackable> actual) {
		for (Trackable current : actual) {
			if (current.equals(expected) && current.sourceUnit == expected.sourceUnit)
				return;
		}
		fail();
	}

	private static <T> Set<T> newIdentitySet(T... obj) {
		Set<T> result = Sets.newIdentityHashSet();
		for (T o : obj) {
			result.add(o);
		}
		return result;
	}
}
