package flow.twist.util;

import java.util.HashMap;
import java.util.Set;

import att.grappa.Edge;
import att.grappa.Node;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import flow.twist.path.Path;
import flow.twist.trackable.Trackable;

public class MultipleAnalysisPlotter extends PathPlotter {

	private HashMap<EdgeKey, Edge> edges = Maps.newHashMap();

	public MultipleAnalysisPlotter() {
		super(Sets.<Path> newHashSet());
	}

	public void plotAnalysisResults(Set<Path> paths, String color) {
		this.currentColor = color;
		for (Path path : paths) {
			createPath(path);
			toggleColor();
		}
	}

	@Override
	protected Edge createEdge(Node fromNode, Node toNode, Trackable trackable, double weight) {
		EdgeKey key = new EdgeKey(fromNode, toNode, trackable);
		if (edges.containsKey(key)) {
			Edge edge = edges.get(key);
			int count = Integer.parseInt((String) edge.getAttributeValue("count"));
			edge.setAttribute("count", String.valueOf(count + 1));
			edge.setAttribute("label", trackable.toString() + " (" + (count + 1) + "x)");
			edge.setAttribute("color", edge.getAttribute("color").getStringValue() + ":" + currentColor());
			edge.setAttribute("fontcolor", "black");

			return edge;
		} else {
			Edge edge = super.createEdge(fromNode, toNode, trackable, weight);
			edge.setAttribute("count", String.valueOf(1));
			edges.put(key, edge);
			return edge;
		}
	}

	private static class EdgeKey {

		private Node fromNode;
		private Node toNode;
		private Trackable trackable;

		private EdgeKey(Node fromNode, Node toNode, Trackable trackable) {
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.trackable = trackable.cloneWithoutNeighborsAndPayload();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromNode == null) ? 0 : System.identityHashCode(fromNode));
			result = prime * result + ((toNode == null) ? 0 : System.identityHashCode(toNode));
			result = prime * result + ((trackable == null) ? 0 : trackable.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EdgeKey other = (EdgeKey) obj;
			if (fromNode != other.fromNode)
				return false;
			if (toNode != other.toNode)
				return false;
			if (!trackable.equals(other.trackable))
				return false;
			return true;
		}
	}
}
