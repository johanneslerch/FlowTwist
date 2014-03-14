package flow.twist;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import flow.twist.path.Path;
import flow.twist.util.AnalysisUtil;
import flow.twist.util.MultipleAnalysisPlotter;

public abstract class AbstractMainAnalysis extends AbstractAnalysis {

	private String[] args;

	public AbstractMainAnalysis(String[] args) {
		this.args = args;
	}

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

	@Override
	protected void executeAnalysis() {
		Set<Path> validPaths = _executeAnalysis();
		System.out.println("Writing reports...");
		writeReportFile(validPaths);
		writeResultGraphs(validPaths);
	}

	private void writeResultGraphs(Set<Path> validPaths) {
		HashMultimap<SootClass, Path> paths = HashMultimap.create();

		for (Path path : validPaths) {
			SootClass declClass = path.context.icfg.getMethodOf(path.getSink()).getDeclaringClass();
			paths.put(declClass, path);
		}

		for (SootClass declClass : paths.keySet()) {
			MultipleAnalysisPlotter plotter = new MultipleAnalysisPlotter();
			plotter.plotAnalysisResults(paths.get(declClass), "blue");

			String filename = "results/graphs/" + declClass.toString().replaceAll("[^\\w]", "_");
			plotter.writeFile(filename);
		}
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

	protected abstract Set<Path> _executeAnalysis();

}
