package flow.twist.trackable;

import heros.solver.LinkedNode;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.Unit;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Trackable implements LinkedNode<Trackable> {

	public final Trackable predecessor;
	public final Unit sourceUnit;
	private final List<Trackable> neighbors;
	private final Set<Object> payload;

	protected Trackable() {
		this.predecessor = null;
		this.sourceUnit = null;
		payload = Sets.newHashSet();
		neighbors = Lists.newLinkedList();
	}

	public Trackable(Unit sourceUnit, Trackable predecessor) {
		this.predecessor = predecessor;
		this.sourceUnit = sourceUnit;
		payload = Sets.newHashSet();
		neighbors = Lists.newLinkedList();
		payload.addAll(predecessor.payload);
	}

	public void addPayload(Object payload) {
		this.payload.add(payload);
	}

	public void addNeighbor(Trackable trackable) {
		// trackable.neighbors.addAll(getSelfAndNeighbors());
		neighbors.add(trackable);
	}

	public Collection<Trackable> getSelfAndNeighbors() {
		LinkedList<Trackable> result = Lists.newLinkedList(neighbors);
		result.add(this);
		return result;
	}

	public boolean hasSelfOrNeighborAnPredecessor() {
		for (Trackable t : getSelfAndNeighbors())
			if (t.predecessor != null)
				return true;
		return false;
	}

	public abstract Trackable cloneWithoutNeighborsAndPayload();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Trackable))
			return false;
		Trackable other = (Trackable) obj;
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		return true;
	}

	public String payloadString() {
		if (payload.size() == 0)
			return "";
		return "(" + Joiner.on(",").join(payload) + ")";
	}

	public abstract Trackable createAlias(Unit sourceUnits);

	public Collection<Object> getPayload() {
		return payload;
	}
}
