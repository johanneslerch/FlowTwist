package flow.twist.test.util;

import static org.mockito.Mockito.mock;

import java.util.Set;

import org.mockito.Mockito;

import soot.Unit;

import com.google.common.collect.Sets;

import flow.twist.trackable.Trackable;

public class TestTaint extends Trackable {

	private Set<TestTaint> equalTaints = Sets.newIdentityHashSet();
	private String identifier;

	public TestTaint(Unit unit, Trackable predecessor, String identifier) {
		super(unit, predecessor);
		this.identifier = identifier;
	}

	public TestTaint(Trackable predecessor, String identifier) {
		this(createUnitMock(identifier), predecessor, identifier);
	}

	protected static Unit createUnitMock(String identifier) {
		Unit mock = mock(Unit.class);
		Mockito.when(mock.toString()).thenReturn("unit(" + identifier + ")");
		return mock;
	}

	@Override
	public Trackable cloneWithoutNeighborsAndPayload() {
		return null;
	}

	@Override
	public Trackable createAlias(Unit sourceUnits) {
		return null;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		return equalTaints.contains(obj);
	}

	@Override
	public String toString() {
		return identifier;
	}

	public static void setEqual(TestTaint... taints) {
		Set<TestTaint> eq = Sets.newIdentityHashSet();
		for (TestTaint t : taints) {
			eq.add(t);
		}
		for (TestTaint tt : taints) {
			tt.equalTaints = eq;
		}
	}
}