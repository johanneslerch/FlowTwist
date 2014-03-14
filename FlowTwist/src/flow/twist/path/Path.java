package flow.twist.path;

import java.util.Iterator;
import java.util.List;

import soot.SootMethod;
import soot.Unit;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import flow.twist.config.AnalysisConfiguration;
import flow.twist.config.AnalysisContext;
import flow.twist.config.AnalysisDirection;
import flow.twist.util.AnalysisUtil;

public class Path implements Iterable<PathElement> {

	private Iterable<PathElement> elements;
	public final AnalysisContext context;
	private final fj.data.List<Object> callStack;
	private Unit sink;

	public Path(AnalysisContext context, Iterable<PathElement> elements, fj.data.List<Object> callStack, Unit sink) {
		this.context = context;
		this.elements = elements;
		this.callStack = callStack;
		this.sink = sink;
	}

	public Unit getFirst() {
		return Iterables.getFirst(elements, null).from;
	}

	public Unit getLast() {
		return Iterables.getLast(elements).to;
	}

	@Override
	public String toString() {
		return Joiner.on("\n").join(elements);
	}

	public String toContextAwareString() {
		StringBuffer buffer = new StringBuffer();

		SootMethod currentMethod = null;
		PathElement lastElement = null;

		for (PathElement e : elements) {
			lastElement = e;
			SootMethod fromMethod = this.context.icfg.getMethodOf(e.from);
			if (currentMethod == null || !currentMethod.equals(fromMethod)) {
				currentMethod = fromMethod;
				buffer.append("[");
				buffer.append(currentMethod);
				buffer.append("]");
				buffer.append("\n");
			}

			buffer.append(AnalysisUtil.getLine(e.from));
			buffer.append("\t");
			buffer.append(e.from);
			buffer.append("\n");

			// buffer.append(e.toString());
			//
			// SootMethod toMethod = this.context.icfg.getMethodOf(e.to);
			// if (currentMethod == null || !currentMethod.equals(toMethod)) {
			// currentMethod = toMethod;
			// buffer.append("\n");
			// buffer.append("[");
			// buffer.append(currentMethod);
			// buffer.append("]");
			// }
			//
			// buffer.append("\n");
		}

		if (lastElement != null) {
			buffer.append(AnalysisUtil.getLine(lastElement.to));
			buffer.append("\t");
			buffer.append(lastElement.to);
			buffer.append("\n");
		}

		return buffer.toString();
	}

	public Path reverse() {
		List<PathElement> newElements = Lists.newLinkedList();
		for (PathElement element : this.elements) {
			newElements.add(0, element.reverse());
		}

		return new Path(reverse(context), newElements, callStack, sink);
	}

	private static AnalysisDirection reverse(AnalysisDirection direction) {
		return direction == AnalysisDirection.FORWARDS ? AnalysisDirection.BACKWARDS : AnalysisDirection.FORWARDS;
	}

	private static AnalysisContext reverse(AnalysisContext context) {
		return new AnalysisContext(new AnalysisConfiguration(context.targets, reverse(context.direction), context.type, context.reporter,
				context.seedFactory, context.debugger, context.propagatorProvider), context.icfg);
	}

	public Path append(Path successor) {
		List<PathElement> elements = Lists.newLinkedList(this.elements);
		Iterables.addAll(elements, successor.elements);
		return new Path(context, elements, callStack, sink);
	}

	@Override
	public Iterator<PathElement> iterator() {
		return elements.iterator();
	}

	public fj.data.List<Object> getCallStack() {
		return callStack;
	}

	public boolean isSubPath(Path other) {
		Iterator<PathElement> otherIt = other.iterator();
		PathElement current = otherIt.next();

		for (PathElement element : elements) {
			if (element.equals(current)) {
				if (otherIt.hasNext())
					current = otherIt.next();
				else
					return true;
			} else
				return false;
		}

		return false;
	}

	public Unit getSink() {
		return sink;
	}
}
