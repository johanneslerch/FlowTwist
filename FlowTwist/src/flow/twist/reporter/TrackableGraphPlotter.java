package flow.twist.reporter;

import static flow.twist.config.AnalysisDirection.FORWARDS;

import java.util.IdentityHashMap;
import java.util.Set;

import soot.SootMethod;
import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import att.grappa.Subgraph;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.trackable.PopFromStack;
import flow.twist.trackable.PushOnStack;
import flow.twist.trackable.Trackable;
import flow.twist.trackable.Zero;
import flow.twist.util.CacheMap;
import flow.twist.util.DotHelper;

public class TrackableGraphPlotter implements IfdsReporter {

	private Graph graph;
	private Set<TaintedEdge> edges = Sets.newHashSet();
	private Set<Trackable> visitedNeighbors = Sets.newIdentityHashSet();

	private CacheMap<SootMethod, Subgraph> methodSubgraphs = new CacheMap<SootMethod, Subgraph>() {
		@Override
		protected Subgraph createItem(SootMethod key) {
			Subgraph subgraph = new Subgraph(graph, "cluster_" + methodSubgraphs.size());
			subgraph.setAttribute("label", "\"" + key + "\"");
			subgraph.setAttribute("style", "filled");
			subgraph.setAttribute("color", "lightgrey");
			return subgraph;
		}
	};

	private IdentityHashMap<Trackable, Node> nodes = new IdentityHashMap<>();

	public Node getOrCreateNode(Trackable trackable) {
		if (!nodes.containsKey(trackable)) {
			Subgraph subgraph = trackable.sourceUnit == null ? graph : methodSubgraphs.getOrCreate(currentContext.icfg
					.getMethodOf(trackable.sourceUnit));
			Node node = new Node(subgraph);
			node.setAttribute("shape", "rectangle");
			String label = trackable.sourceUnit + " \\n " + trackable + " " + System.identityHashCode(trackable);
			if (trackable instanceof PopFromStack)
				label += "\\n Pop: " + ((PopFromStack) trackable).getCallSite();
			if (trackable instanceof PushOnStack)
				label += "\\n Push: " + ((PushOnStack) trackable).getCallSite();

			node.setAttribute("label", label);
			if (trackable instanceof Zero) {
				node.setAttribute("style", "filled");
				node.setAttribute("fillcolor", "green");
			}
			subgraph.addNode(node);
			nodes.put(trackable, node);
		}
		return nodes.get(trackable);
	}

	private AnalysisContext currentContext;

	public TrackableGraphPlotter() {
		graph = new Graph("debug graph");
		graph.setAttribute("compound", "true");
	}

	@Override
	public void reportTrackable(Report report) {
		this.currentContext = report.context;

		Node node = getOrCreateNode(report.trackable);
		node.setAttribute("style", "filled");
		node.setAttribute("fillcolor", report.context.direction == FORWARDS ? "orangered" : "lightblue");
		createNeighborEdges(report.trackable, report.trackable);
	}

	private void createPredecessorEdges(Trackable trackable) {
		if (trackable.predecessor != null) {
			TaintedEdge taintedEdge = new TaintedEdge(trackable, trackable.predecessor);
			if (edges.add(taintedEdge)) {
				Edge edge = new Edge(graph, getOrCreateNode(trackable.predecessor), getOrCreateNode(trackable));
				edge.setAttribute("color", (currentContext.direction == FORWARDS) ? "red" : "blue");
				graph.addEdge(edge);

				createNeighborEdges(trackable, trackable.predecessor);
			}
		}
	}

	private void createNeighborEdges(Trackable succ, Trackable trackable) {
		if (!visitedNeighbors.add(trackable))
			return;

		for (Trackable neighbor : trackable.getSelfAndNeighbors()) {
			if (trackable != neighbor) {
				Edge edge = new Edge(graph, getOrCreateNode(neighbor), getOrCreateNode(succ));
				edge.setAttribute("style", "dashed");
				edge.setAttribute("color", (currentContext.direction == FORWARDS) ? "red" : "blue");
				if (succ == trackable) {
					edge.setAttribute("dir", "none");
					edge.setAttribute("constraint", "false");
				}
			}
			createPredecessorEdges(neighbor);
		}
	}

	@Override
	public void analysisFinished() {
	}

	public void writeFile(String filename) {
		DotHelper.writeFilesForGraph(filename, graph);
	}

	private static class TaintedEdge {

		private Trackable from;
		private Trackable to;

		public TaintedEdge(Trackable from, Trackable to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : System.identityHashCode(from));
			result = prime * result + ((to == null) ? 0 : System.identityHashCode(to));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof TaintedEdge))
				return false;
			TaintedEdge other = (TaintedEdge) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (from != other.from)
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (to != other.to)
				return false;
			return true;
		}

	}

}
