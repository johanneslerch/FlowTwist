package flow.twist.util;

import java.util.Set;

import flow.twist.config.AnalysisContext;
import flow.twist.path.Path;
import flow.twist.path.PathElement;
import flow.twist.trackable.ReturnEdgeTaint;
import flow.twist.trackable.Trackable;
import soot.SootMethod;
import soot.Unit;
import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import att.grappa.Subgraph;

public class PathPlotter {

	private Graph graph;

	private CacheMap<SootMethod, Subgraph> methodSubgraphs = new CacheMap<SootMethod, Subgraph>() {
		@Override
		protected Subgraph createItem(SootMethod key) {
			Subgraph subgraph = new Subgraph(graph, "cluster_" + methodSubgraphs.size());
			subgraph.setAttribute("label", "\"" + key.toString() + "\"");
			subgraph.setAttribute("style", "filled");
			subgraph.setAttribute("color", "lightgrey");
			subgraph.setAttribute("fillcolor", "lightgrey");
			return subgraph;
		}
	};

	private CacheMap<Unit, Node> nodes = new CacheMap<Unit, Node>() {
		@Override
		protected Node createItem(Unit key) {
			SootMethod enclosingMethod = context.icfg.getMethodOf(key);
			Subgraph subgraph = methodSubgraphs.getOrCreate(enclosingMethod);
			Node node = new Node(subgraph);
			node.setAttribute("shape", "rectangle");
			node.setAttribute("label", key.toString());
			if (AnalysisUtil.isTarget(context.direction, enclosingMethod, key, context.targets)) {
				node.setAttribute("style", "filled");
				node.setAttribute("fillcolor", "green");
			}
			subgraph.addNode(node);

			return node;
		}
	};

	protected AnalysisContext context;
	private int colorIndex = 0;
	private String[] colors = new String[] { "red", "blue", "green", "yellow", "cyan", "orange" };
	protected String currentColor = colors[0];

	public PathPlotter(Set<Path> paths) {
		graph = new Graph("debug graph");
		graph.setAttribute("compound", "true");
		for (Path path : paths) {
			createPath(path);

			toggleColor();
		}
	}

	protected void toggleColor() {
		colorIndex = (colorIndex + 1) % colors.length;
		currentColor = colors[colorIndex];
	}

	protected void createPath(Path path) {
		context = path.context;
		for (PathElement element : path) {
			Node fromNode = nodes.getOrCreate(element.from);
			Node toNode = nodes.getOrCreate(element.to);

			if (element.trackable instanceof ReturnEdgeTaint) {
				ReturnEdgeTaint retTaint = (ReturnEdgeTaint) element.trackable;
				Node callSiteNode = nodes.getOrCreate(retTaint.callSite);
				Edge edge = createEdge(fromNode, callSiteNode, element.trackable, 1);
				createEdge(callSiteNode, toNode, element.trackable, calculateEdgeWeight(retTaint.callSite, element.to));
			} else {
				createEdge(fromNode, toNode, element.trackable, calculateEdgeWeight(element.from, element.to));
			}
		}
		nodes.getOrCreate(path.getFirst()).getSubgraph().setAttribute("color", "green");
	}

	private double calculateEdgeWeight(Unit from, Unit to) {
		if (context.icfg.getMethodOf(from).equals(context.icfg.getMethodOf(to))) {
			return 100;
		} else
			return 5;
	}

	protected String currentColor() {
		return currentColor;
	}

	protected Edge createEdge(Node fromNode, Node toNode, Trackable trackable, double weight) {
		Edge edge = new Edge(graph, fromNode, toNode);
		edge.setAttribute("label", trackable.toString());
		edge.setAttribute("color", currentColor());
		edge.setAttribute("fontcolor", currentColor());
		edge.setAttribute("weight", weight);
		graph.addEdge(edge);
		return edge;
	}

	public void writeFile(String filename) {
		DotHelper.writeFilesForGraph(filename, graph);
	}

}
