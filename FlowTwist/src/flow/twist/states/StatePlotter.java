package flow.twist.states;

import java.util.Set;

import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;

import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.states.StateCache.ContextStateCache;
import flow.twist.states.StateCache.StatePushNode;
import flow.twist.states.StateCache.StateSinkNode;
import flow.twist.states.StateCache.StateStartNode;
import flow.twist.util.CacheMap;
import flow.twist.util.DotHelper;

public class StatePlotter {

	private StateMachineBasedPathReporter reporter;
	private Graph graph;
	private CacheMap<StateNode, Node> nodes = new CacheMap<StateNode, Node>() {
		@Override
		protected Node createItem(StateNode key) {
			Node node = new Node(graph);
			node.setAttribute("shape", "rectangle");
			node.setAttribute("label", createLabel(key));

			if (key instanceof StateStartNode) {
				node.setAttribute("style", "filled");
				node.setAttribute("fillcolor", "red");
			}
			if (key instanceof StateSinkNode) {
				node.setAttribute("style", "filled");
				node.setAttribute("fillcolor", "green");
			}
			graph.addNode(node);
			return node;
		}

		private String createLabel(StateNode key) {
			if (key instanceof StateStartNode)
				return ((StateStartNode) key).method.toString();
			else if (key instanceof StateSinkNode)
				return ((StateSinkNode) key).unit.toString();
			else if (key instanceof StatePushNode)
				return ((StatePushNode) key).methodName;
			else
				return "return";
		}
	};
	private Set<StateTransition> visitedTransitions = Sets.newHashSet();

	public StatePlotter(StateMachineBasedPathReporter reporter) {
		this.reporter = reporter;
	}

	public void write(String filename) {
		graph = new Graph("debug graph");
		graph.setAttribute("compound", "true");

		for (ContextStateCache stateCache : reporter.getStateCache().getAll()) {
			for (StateNode state : stateCache.getAllStartStates()) {
				plot(state, stateCache.context);
			}
		}

		DotHelper.writeFilesForGraph(filename, graph);
	}

	private void plot(StateNode state, AnalysisContext context) {
		for (StateTransition t : state.getOutgoing()) {
			if (visitedTransitions.add(t)) {
				plot(t.getTarget(), context);
				createEdge(t, context);
			}
		}
	}

	private void createEdge(StateTransition t, AnalysisContext context) {
		Node from = nodes.getOrCreate(t.getSource());
		Node to = nodes.getOrCreate(t.getTarget());

		Edge edge = new Edge(graph, from, to);
		String label = t.toString().replaceAll("\n", "\\\\l");
		edge.setAttribute("label", t.condition + " Push: " + t.isPushOnStack() + "\\l" + label);
		edge.setAttribute("color", context.direction == AnalysisDirection.FORWARDS ? "red" : "blue");
		edge.setAttribute("fontcolor", context.direction == AnalysisDirection.FORWARDS ? "red" : "blue");
		graph.addEdge(edge);
	}
}
