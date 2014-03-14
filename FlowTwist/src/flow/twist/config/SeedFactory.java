package flow.twist.config;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import flow.twist.ActiveBodyVerifier;
import flow.twist.targets.AnalysisTarget;
import flow.twist.util.AnalysisUtil;

public interface SeedFactory {

	public Set<Unit> initialSeeds(AnalysisContext config);

	public static class I2OSeedFactory implements SeedFactory {

		@Override
		public Set<Unit> initialSeeds(AnalysisContext config) {
			Set<Unit> res = Sets.newHashSet();
			for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
				SootMethod m = iter.next().method();
				if (m.hasActiveBody() && !m.getName().equals("class$")) {
					ActiveBodyVerifier.assertActive(m);
					PatchingChain<Unit> units = m.getActiveBody().getUnits();
					for (Unit u : units) {
						Stmt s = (Stmt) u;
						if (s.containsInvokeExpr()) {
							InvokeExpr ie = s.getInvokeExpr();
							if (isSink(config, m, s, ie)) {
								res.add(u);
							}
						}
					}
				}
			}
			System.out.println("inner to outer initial seeds for direction " + config.direction + ": " + res.size());
			return res;
		}

		private boolean isSink(AnalysisConfiguration config, SootMethod enclosingMethod, Stmt s, InvokeExpr ie) {
			return matchesAtLeastOneTarget(config, enclosingMethod, ie);
		}

		private boolean matchesAtLeastOneTarget(AnalysisConfiguration config, SootMethod enclosingMethod, InvokeExpr ie) {
			for (AnalysisTarget t : config.targets)
				if (t.matches(config.direction, enclosingMethod, ie))
					return true;
			return false;
		}
	}

	public class AllParameterOfTransitiveCallersSeedFactory implements SeedFactory {

		@Override
		public Set<Unit> initialSeeds(AnalysisContext config) {
			List<Unit> worklist = Lists.newLinkedList((new I2OSeedFactory().initialSeeds(config)));
			Set<SootMethod> visited = Sets.newHashSet();
			Set<Unit> result = Sets.newHashSet();

			while (!worklist.isEmpty()) {
				Unit current = worklist.remove(0);
				SootMethod method = config.icfg.getMethodOf(current);
				if (visited.add(method)) {
					if (AnalysisUtil.methodMayBeCallableFromApplication(method)) {
						PatchingChain<Unit> units = method.getActiveBody().getUnits();
						for (Unit u : units) {
							if (u instanceof IdentityStmt) {
								IdentityStmt idStmt = (IdentityStmt) u;

								if (idStmt.getRightOp().toString().contains("@parameter"))
									result.add(u);
							}
						}
					} else {
						worklist.addAll(config.icfg.getCallersOf(method));
					}
				}
			}

			System.out.println("forwards from all transitive callers parameters initial seeds: " + result.size());
			return result;
		}
	}

	public class AllParameterOfCallableMethodsSeedFactory implements SeedFactory {

		@Override
		public Set<Unit> initialSeeds(AnalysisContext config) {
			Set<Unit> res = Sets.newHashSet();
			for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
				SootMethod m = iter.next().method();
				if (AnalysisUtil.methodMayBeCallableFromApplication(m) && m.hasActiveBody()) {
					ActiveBodyVerifier.assertActive(m);
					PatchingChain<Unit> units = m.getActiveBody().getUnits();
					for (Unit u : units) {
						if (u instanceof IdentityStmt) {
							IdentityStmt idStmt = (IdentityStmt) u;

							if (idStmt.getRightOp().toString().contains("@parameter"))
								res.add(u);
						}
					}
				}
			}
			System.out.println("forwards from all parameters initial seeds: " + res.size());
			return res;
		}
	}

	public class StringParameterOfTransitiveCallersSeedFactory implements SeedFactory {

		@Override
		public Set<Unit> initialSeeds(AnalysisContext config) {
			List<Unit> worklist = Lists.newLinkedList((new I2OSeedFactory().initialSeeds(config)));
			Set<SootMethod> visited = Sets.newHashSet();
			Set<Unit> result = Sets.newHashSet();

			while (!worklist.isEmpty()) {
				Unit current = worklist.remove(0);
				SootMethod method = config.icfg.getMethodOf(current);
				if (visited.add(method)) {
					if (AnalysisUtil.methodMayBeCallableFromApplication(method)) {
						PatchingChain<Unit> units = method.getActiveBody().getUnits();
						for (Unit u : units) {
							if (u instanceof IdentityStmt) {
								IdentityStmt idStmt = (IdentityStmt) u;

								if (idStmt.getRightOp().toString().contains("@parameter")
										&& idStmt.getRightOp().getType().toString().equals("java.lang.String"))
									result.add(u);
							}
						}
					} else {
						worklist.addAll(config.icfg.getCallersOf(method));
					}
				}
			}

			System.out.println("forwards from string transitive callers parameters initial seeds: " + result.size());
			return result;
		}
	}

	public class StringParameterOfCallableMethodsSeedFactory implements SeedFactory {

		@Override
		public Set<Unit> initialSeeds(AnalysisContext config) {
			Set<Unit> res = Sets.newHashSet();
			for (Iterator<MethodOrMethodContext> iter = Scene.v().getReachableMethods().listener(); iter.hasNext();) {
				SootMethod m = iter.next().method();
				if (AnalysisUtil.methodMayBeCallableFromApplication(m) && m.hasActiveBody()) {
					ActiveBodyVerifier.assertActive(m);
					PatchingChain<Unit> units = m.getActiveBody().getUnits();
					for (Unit u : units) {
						if (u instanceof IdentityStmt) {
							IdentityStmt idStmt = (IdentityStmt) u;

							if (idStmt.getRightOp().toString().contains("@parameter")
									&& idStmt.getRightOp().getType().toString().equals("java.lang.String"))
								res.add(u);
						}
					}
				}
			}
			System.out.println("forwards from string parameters initial seeds: " + res.size());
			return res;
		}
	}
}
