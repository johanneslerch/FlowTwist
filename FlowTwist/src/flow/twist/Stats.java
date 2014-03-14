package flow.twist;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import com.google.common.collect.Sets;

import flow.twist.util.AnalysisUtil;

public class Stats {

	public static void print() {
		Set<SootMethod> _reachableMethods = Sets.newHashSet();
		Set<SootMethod> _methods = Sets.newHashSet();

		{
			int reachable = 0;
			int reachableAndCallable = 0;
			int concrete = 0;
			int reachableAndCallableWithBody = 0;
			Set<SootClass> reachableClasses = Sets.newHashSet();

			for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
				SootMethod m = iter.next().method();
				if (m.isPhantom())
					continue;
				reachable++;

				if (m.isConcrete()) {
					_reachableMethods.add(m);
					concrete++;
				}

				if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
					reachableAndCallable++;
					if (m.hasActiveBody())
						reachableAndCallableWithBody++;
				}

				reachableClasses.add(m.getDeclaringClass());
			}
			writeFile(reachableClasses);
			System.out.println("Reachable methods: " + reachable);
			System.out.println("Reachable and concrete methods: " + concrete);
			System.out.println("Reachable and callable methods: " + reachableAndCallable);
			System.out.println("Reachable and callable methods with body: " + reachableAndCallableWithBody);
			System.out.println("Reachable classes: " + reachableClasses.size());
		}

		{
			int classes = 0;
			int methods = 0;
			int callableMethods = 0;
			int reachableAndCallableWithBody = 0;
			int callableAndConcrete = 0;
			int concrete = 0;
			for (SootClass c : Scene.v().getClasses()) {
				if (c.isPhantomClass())
					continue;

				classes++;
				for (SootMethod m : c.getMethods()) {
					if (m.isPhantom())
						continue;

					if (m.isConcrete()) {
						concrete++;
						_methods.add(m);
					}

					methods++;
					if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
						callableMethods++;
						if (m.hasActiveBody())
							reachableAndCallableWithBody++;
						if (m.isConcrete())
							callableAndConcrete++;
					}
				}
			}

			System.out.println("Total methods: " + methods);
			System.out.println("Total concrete methods: " + concrete);
			System.out.println("Total callable methods: " + callableMethods);
			System.out.println("Total callable methods with body: " + reachableAndCallableWithBody);
			System.out.println("Total callable concrete methods: " + callableAndConcrete);
			System.out.println("Total classes: " + classes);
		}

		{
			int classes = 0;
			int methods = 0;
			int callableMethods = 0;
			for (SootClass c : Scene.v().getLibraryClasses()) {

				if (c.isPhantomClass())
					continue;

				classes++;
				for (SootMethod m : c.getMethods()) {
					if (m.isPhantom())
						continue;

					methods++;
					if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
						callableMethods++;
					}
				}
			}

			System.out.println("Library methods: " + methods);
			System.out.println("Library callable methods: " + callableMethods);
			System.out.println("Library classes: " + classes);
		}

		{
			int classes = 0;
			int methods = 0;
			int callableMethods = 0;
			for (SootClass c : Scene.v().getApplicationClasses()) {

				if (c.isPhantomClass())
					continue;

				classes++;
				for (SootMethod m : c.getMethods()) {
					if (m.isPhantom())
						continue;

					methods++;
					if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
						callableMethods++;
					}
				}
			}

			System.out.println("Application methods: " + methods);
			System.out.println("Application callable methods: " + callableMethods);
			System.out.println("Application classes: " + classes);
		}

		{
			int classes = 0;
			int methods = 0;
			int callableMethods = 0;
			for (SootClass c : Scene.v().getPhantomClasses()) {
				classes++;
				for (SootMethod m : c.getMethods()) {
					if (!m.isPhantom())
						continue;

					methods++;
					if (AnalysisUtil.methodMayBeCallableFromApplication(m)) {
						callableMethods++;
					}
				}
			}

			System.out.println("Phantom methods: " + methods);
			System.out.println("Phantom callable methods: " + callableMethods);
			System.out.println("Phantom classes: " + classes);
		}

		// System.out.println("Unreachable methods:");
		// int i = 0;
		// for (SootMethod m : _methods) {
		// if (!_reachableMethods.contains(m)) {
		// System.out.println((++i) + " " + m);
		// }
		// }
	}

	private static void writeFile(Set<SootClass> reachableClasses) {
		try {
			TreeSet<String> treeSet = new TreeSet<>();
			for (SootClass sootClass : reachableClasses) {
				treeSet.add(sootClass.toString());
			}

			FileWriter writer = new FileWriter(new File("reachableClasses.txt"));
			for (String sootClass : treeSet) {
				writer.write(sootClass + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
