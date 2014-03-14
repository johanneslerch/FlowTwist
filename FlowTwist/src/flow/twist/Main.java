package flow.twist;

import static flow.twist.config.AnalysisConfigurationBuilder.forwardsFromAllParametersDefaults;
import static flow.twist.config.AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults;
import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.VoidType;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import flow.twist.config.AnalysisConfigurationBuilder;
import flow.twist.path.Path;
import flow.twist.pathchecker.FilterSingleDirectionReports;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.targets.GenericCallerSensitive;
import flow.twist.transformer.StoreDataTransformer;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;
import flow.twist.util.AnalysisUtil;
import flow.twist.util.MultipleAnalysisPlotter;

public class Main {

	public static void main(final String[] args) {
		new AbstractAnalysis() {

			@Override
			protected void executeAnalysis() {

				// executeMultipleAnalysis();

				printCallerSensitiveStats();

				MultipleAnalysisPlotter plotter = new MultipleAnalysisPlotter();

				// Set<Path> validPaths = executeI2OForNameAnalysis();
				// Set<Path> validPaths = executeBidiForNameAnalysis();
				// Set<Path> validPaths = executeBidiCallerSensitiveAnalysis();
				Set<Path> validPaths = executeForwardOnlyForNameAnalysis();

				plotter.plotAnalysisResults(validPaths, "blue");
				plotter.writeFile("analysisResults");

				// SolverFactory.runInnerToOuterSolver(new ConsoleReporter());
				// SolverFactory.runI2OOneDirectionSolver(AnalysisDirection.BACKWARDS,
				// new ConsoleReporter());

				// executeSinkBySink();

				// Debugger debugger = new Debugger();
				// FanInFanOutDebugger fanInOut = new FanInFanOutDebugger();
				// debugger.registerListener(fanInOut);
				// TabularViewer tabularViewer = new TabularViewer(debugger,
				// fanInOut);
				// SolverFactory.runOneDirectionSolver(AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults()
				// .direction(AnalysisDirection.FORWARDS).reporter(new
				// ConsoleReporter()).debugger(debugger));
				// tabularViewer.dispose();

				writeReportFile(validPaths);
			}

			private void writeReportFile(Set<Path> validPaths) {
				Multimap<SootMethod, Path> pathsBySink = HashMultimap.create();
				for (Path path : validPaths) {
					pathsBySink.put(path.context.icfg.getMethodOf(path.getSink()), path);
				}

				try {
					FileWriter writer = new FileWriter(new File("report.csv"));
					for (SootMethod sink : pathsBySink.keySet()) {
						writer.write(String.format("%s;%d\n", sink.toString(), pathsBySink.get(sink).size()));
					}
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			private void executeMultipleAnalysis() {
				AnalysisReporting.directory = new File("results/forName/bidi");
				SolverFactory.runBiDirectionSolver(AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(new StoreDataTransformer<Set<Path>>(),
								new MergeEqualSelectorStrategy()))));

				AnalysisReporting.directory = new File("results/forName/i2o-filtered");
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new FilterSingleDirectionReports(new PathBuilderResultTransformer(
								new StoreDataTransformer<Set<Path>>(), new MergeEqualSelectorStrategy())))));

				AnalysisReporting.directory = new File("results/forName/i2o");
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(new StoreDataTransformer<Set<Path>>(),
								new MergeEqualSelectorStrategy()))));

				AnalysisReporting.directory = new File("results/callersens/bidi");
				SolverFactory.runBiDirectionSolver(AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(new StoreDataTransformer<Set<Path>>(),
								new MergeEqualSelectorStrategy()))));

				AnalysisReporting.directory = new File("results/callersens/i2o-filtered");
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults().reporter(
						new ResultForwardingReporter(new FilterSingleDirectionReports(new PathBuilderResultTransformer(
								new StoreDataTransformer<Set<Path>>(), new MergeEqualSelectorStrategy())))));

				AnalysisReporting.directory = new File("results/callersens/i2o");
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oGenericCallerSensitiveDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(new StoreDataTransformer<Set<Path>>(),
								new MergeEqualSelectorStrategy()))));
			}

			private void printCallerSensitiveStats() {
				Set<SootMethod> methods = new GenericCallerSensitive().getSensitiveMethods();
				System.out.println("Total caller sensitive methods: " + methods.size());
				int integrityOnly = 0;
				int confidentialityOnly = 0;
				for (SootMethod m : methods) {
					boolean confidentiality = false;
					boolean integrity = false;

					if (!m.isStatic())
						integrity = true;

					if (m.getParameterCount() > 0)
						integrity = true;

					if (!(m.getReturnType() instanceof VoidType))
						confidentiality = true;

					if (confidentiality && !integrity) {
						confidentialityOnly++;
						System.out.println(m);
					} else if (integrity && !confidentiality) {
						integrityOnly++;
						System.out.println(m);
					}
				}

				System.out.println("Integrity only: " + integrityOnly);
				System.out.println("Confidentiality only: " + confidentialityOnly);

				int total = 0;
				for (SootClass cl : Scene.v().getClasses()) {
					for (SootMethod m : cl.getMethods()) {
						for (Tag tag : m.getTags()) {
							if (tag instanceof VisibilityAnnotationTag) {
								for (AnnotationTag annotation : ((VisibilityAnnotationTag) tag).getAnnotations()) {
									if (annotation.getType().equals("Lsun/reflect/CallerSensitive;")) {
										total++;
										if (methods.contains(m))
											methods.remove(m);
										else
											System.out.println("Cross check found method not in AnalysisTarget: " + m);
									}
								}
							}
						}
					}
				}
				System.out.println("cross check: " + total);
				System.out.println(methods.size());
			}

			private Set<Path> executeForwardOnlyForNameAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runOneDirectionSolver(AnalysisConfigurationBuilder.forwardsFromAllParametersDefaults(false).reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage, new MergeEqualSelectorStrategy()))));
				return dataStorage.getData();
			}

			private Set<Path> executeForwardOnlyCallerSensitiveAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runOneDirectionSolver(forwardsFromAllParametersDefaults(true).reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage, new MergeEqualSelectorStrategy()))));
				return dataStorage.getData();
			}

			private Set<Path> executeI2OForNameAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runInnerToOuterSolver(AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new FilterSingleDirectionReports(new PathBuilderResultTransformer(dataStorage,
								new MergeEqualSelectorStrategy())))));
				return dataStorage.getData();
			}

			private Set<Path> executeBidiForNameAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runBiDirectionSolver(i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage, new MergeEqualSelectorStrategy()))));
				return dataStorage.getData();
			}

			private Set<Path> executeBidiCallerSensitiveAnalysis() {
				StoreDataTransformer<Set<Path>> dataStorage = new StoreDataTransformer<Set<Path>>();
				SolverFactory.runBiDirectionSolver(i2oGenericCallerSensitiveDefaults().reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(dataStorage, new MergeEqualSelectorStrategy()))));
				return dataStorage.getData();
			}

			// private void executeSinkBySink() {
			// AnalysisConfigurationBuilder configBuilder =
			// i2oGenericCallerSensitiveDefaults().direction(FORWARDS).reporter(new
			// ConsoleReporter());
			// Set<Unit> seeds = new
			// SeedFactory.I2OSeedFactory().initialSeeds(configBuilder.build());
			// int i = 0;
			// for (final Unit seed : seeds) {
			// System.out.println("===============");
			// System.out.println("Seed: " + i + " / " + seeds.size());
			// System.out.println("===============");
			//
			// Set<Path> validPaths = executeWithTimeout(configBuilder, seed);
			//
			// System.out.println("Solvers finished");
			//
			// if (!validPaths.isEmpty()) {
			// MultipleAnalysisPlotter plotter = new MultipleAnalysisPlotter();
			// plotter.plotAnalysisResults(validPaths, "red");
			// plotter.writeFile("analysisResults-" + i);
			// }
			// i++;
			// }
			// }

			// protected Set<Path>
			// executeWithTimeout(AnalysisConfigurationBuilder configBuilder,
			// final Unit seed) {
			// StoreDataTransformer<Set<Path>> dataStorage = new
			// StoreDataTransformer<Set<Path>>();
			// SolverFactory.runInnerToOuterSolver(configBuilder.reporter(
			// new ResultForwardingReporter(new FilterSingleDirectionReports(new
			// PathBuilderResultTransformer(dataStorage,
			// new
			// MergeEqualSelectorStrategy())))).seedFactory(createSeedFactory(seed)));
			// return dataStorage.getData();
			// }
			//
			// private void execute(AnalysisConfigurationBuilder configBuilder)
			// {
			// Debugger debugger = new Debugger();
			// FanInFanOutDebugger fanInOut = new FanInFanOutDebugger();
			// debugger.registerListener(fanInOut);
			// // TabularViewer tabularViewer = new TabularViewer(debugger,
			// // fanInOut);
			// SolverFactory.runOneDirectionSolver(configBuilder.debugger(debugger));
			// // tabularViewer.dispose();
			// }

			// protected SeedFactory createSeedFactory(final Unit seed) {
			// return new SeedFactory() {
			// @Override
			// public java.util.Set<Unit> initialSeeds(AnalysisConfiguration
			// config) {
			// return Sets.newHashSet(seed);
			// };
			// };
			// }

			@Override
			protected ArrayList<String> createArgs() {
				ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
				String jrePath = argList.remove(0);
				AnalysisUtil.initRestrictedPackages(jrePath);

				argList.add("-w");

				argList.add("-f");
				argList.add("none");

				argList.add("-p");
				argList.add("cg");
				argList.add("all-reachable:true");

				argList.add("-keep-line-number");

				argList.add("-include-all");
				argList.add("-allow-phantom-refs");

				argList.add("-f");
				argList.add("none");

				argList.add("-pp");

				return argList;
			}
		}.execute();
	}
}
