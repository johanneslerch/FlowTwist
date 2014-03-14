package flow.twist.reporter;

import static flow.twist.config.AnalysisDirection.FORWARDS;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import att.grappa.Subgraph;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.trackable.ReturnEdgeTaint;
import flow.twist.trackable.Trackable;
import flow.twist.util.AnalysisUtil;
import flow.twist.util.CacheMap;
import flow.twist.util.DotHelper;

public class DebugPlotter implements IfdsReporter {

	private Graph graph;
	private Set<TaintedEdge> edges = Sets.newHashSet();

	private CacheMap<SootMethod, Subgraph> methodSubgraphs = new CacheMap<SootMethod, Subgraph>() {
		@Override
		protected Subgraph createItem(SootMethod key) {
			Subgraph subgraph = new Subgraph(graph, "cluster_" + methodSubgraphs.size());
			subgraph.setAttribute("label", "\"" + key.toString() + "\"");
			subgraph.setAttribute("style", "filled");
			subgraph.setAttribute("color", "lightgrey");
			return subgraph;
		}
	};

	private CacheMap<Unit, Node> nodes = new CacheMap<Unit, Node>() {
		@Override
		protected Node createItem(Unit key) {
			SootMethod enclosingMethod = currentContext.icfg.getMethodOf(key);
			Subgraph subgraph = methodSubgraphs.getOrCreate(enclosingMethod);
			Node node = new Node(subgraph);
			node.setAttribute("shape", "rectangle");
			node.setAttribute("label", key.toString());
			if (AnalysisUtil.isTarget(currentContext.direction, enclosingMethod, key, currentContext.targets)) {
				node.setAttribute("style", "filled");
				node.setAttribute("fillcolor", "green");
			}
			subgraph.addNode(node);

			return node;
		}
	};

	private AnalysisContext currentContext;
	private PlotFilter filter;

	public DebugPlotter() {
		graph = new Graph("debug graph");
		graph.setAttribute("compound", "true");
	}

	public DebugPlotter(PlotFilter filter) {
		this();
		this.filter = filter;
	}

	@Override
	public void reportTrackable(Report report) {
		this.currentContext = report.context;
		for (Trackable neighbor : report.trackable.getSelfAndNeighbors())
			createEdges(neighbor, report.targetUnit);

		Node sourceNode = nodes.getOrCreate(report.targetUnit);
		sourceNode.setAttribute("style", "filled");
		sourceNode.setAttribute("fillcolor", report.context.direction == FORWARDS ? "red" : "blue");
	}

	@Override
	public void analysisFinished() {
	}

	private void createEdges(Trackable trackable, Unit to) {
		if (filter != null && !filter.include(currentContext, to))
			return;

		for (Trackable neighbor : trackable.getSelfAndNeighbors()) {
			Unit currentTo = to;

			if (neighbor.sourceUnit == null)
				continue;

			if (neighbor instanceof ReturnEdgeTaint) {
				ReturnEdgeTaint retTaint = (ReturnEdgeTaint) neighbor;
				createEdge(retTaint.callSite, currentTo, neighbor);
				currentTo = retTaint.callSite;
			}

			if (createEdge(neighbor.sourceUnit, currentTo, neighbor))
				createEdges(neighbor.predecessor, neighbor.sourceUnit);
		}
	}

	private boolean createEdge(Unit from, Unit to, Trackable trackable) {
		if (hasEdge(from, to, trackable))
			return false;

		edges.add(new TaintedEdge(from, trackable, to));

		Edge edge = new Edge(graph, nodes.getOrCreate(from), nodes.getOrCreate(to));
		edge.setAttribute("label", trackable.toString());
		edge.setAttribute("color", (currentContext.direction == FORWARDS) ? "red" : "blue");
		edge.setAttribute("fontcolor", (currentContext.direction == FORWARDS) ? "red" : "blue");
		if (trackable instanceof ReturnEdgeTaint) {
			edge.setAttribute("arrowhead", "onormal");
		}
		graph.addEdge(edge);
		return true;
	}

	private boolean hasEdge(Unit from, Unit to, Trackable trackable) {
		return edges.contains(new TaintedEdge(from, trackable, to));
	}

	public void writeFile(String filename) {
		DotHelper.writeFilesForGraph(filename, graph);
	}

	private static class TaintedEdge {
		public final Trackable trackable;
		public final Unit source;
		public final Unit target;

		public TaintedEdge(Unit source, Trackable trackable, Unit target) {
			this.source = source;
			this.trackable = trackable;
			this.target = target;

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((trackable == null) ? 0 : System.identityHashCode(trackable));
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
			TaintedEdge other = (TaintedEdge) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			if (trackable != other.trackable) {
				return false;
			}
			return true;
		}
	}

	public static interface PlotFilter {

		boolean include(AnalysisContext currentContext, Unit to);

	}
}
