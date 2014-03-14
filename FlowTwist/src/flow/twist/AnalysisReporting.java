package flow.twist;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import flow.twist.config.AnalysisContext;
import flow.twist.path.Path;
import flow.twist.reporter.Report;

public class AnalysisReporting {

	public static File directory = new File("results");
	private static Stopwatch init;
	private static List<String> sootArgs;
	private static Stopwatch analysis;
	private static Map<Object, IfdsData> solverData;
	private static Map<AnalysisContext, IfdsData> ifdsContextData;
	private static Map<Report, ReportData> reports;
	private static Stopwatch combinedPaths;
	private static Set<Path> createdCombinedPaths;
	private static String statsFile = getStatsFileName();

	private static void reset() {
		analysis = new Stopwatch();
		solverData = Maps.newHashMap();
		ifdsContextData = Maps.newHashMap();
		reports = Maps.newHashMap();
		combinedPaths = new Stopwatch();
		createdCombinedPaths = Sets.newHashSet();
	}

	private static String getStatsFileName() {
		return "stats.csv";
	}

	public static void setSootArgs(List<String> sootArgs) {
		init = new Stopwatch();
		init.start();
		AnalysisReporting.sootArgs = sootArgs;
	}

	public static void analysisStarted() {
		if (init.isRunning())
			init.stop();
		reset();
		analysis.start();
	}

	public static void analysisFinished() {
		analysis.stop();

		directory.mkdirs();
		writeGeneralInfoFile();
		writeSemiPaths();
		writeValidPaths();
		writeStatistics();
	}

	public static void ifdsStarting(AnalysisContext context, Object solver) {
		if (!solverData.containsKey(solver)) {
			IfdsData data = new IfdsData();
			data.watch.start();
			solverData.put(solver, data);
		}

		{
			IfdsData data = new IfdsData();
			data.watch.start();
			ifdsContextData.put(context, data);
		}
	}

	public static void ifdsFinished(AnalysisContext context, Object solver, int propagations) {
		{
			IfdsData data = solverData.get(solver);
			if (data.watch.isRunning()) {
				data.watch.stop();
				data.solver = solver;
				data.propagations += propagations;
			}
		}
		{
			IfdsData data = ifdsContextData.get(context);
			data.solver = solver;
			data.watch.stop();
			data.propagations = propagations;

			System.out.println("IFDS finished (" + context.direction + ") in: " + format(data.watch.elapsed(MILLISECONDS)) + " and " + propagations
					+ " propagations.");
		}
	}

	public static void startingPathsFor(Report report) {
		ReportData data = new ReportData();
		reports.put(report, data);
		data.watch.start();
	}

	public static void finishedPathsFor(Report report, Collection<Path> paths) {
		ReportData data = reports.get(report);
		data.watch.stop();
		data.paths = paths;
	}

	public static void combinedPathsStarting() {
		combinedPaths.start();
	}

	public static void combinedPathsFinished(Set<Path> validPaths) {
		combinedPaths.stop();
		createdCombinedPaths = validPaths;
	}

	private static void writeStatistics() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, statsFile), true));
			writer.write(String.valueOf(getMaxHeapSize()));
			writer.write(";");
			writer.write(String.valueOf(analysis.elapsed(MILLISECONDS)));
			writer.write(";");
			writer.write(String.valueOf(init.elapsed(MILLISECONDS)));
			writer.write(";");
			writer.write(String.valueOf(totalIfds()));
			writer.write(";");
			writer.write(String.valueOf(totalPathCreation()));
			writer.write(";");
			writer.write(String.valueOf(combinedPaths.elapsed(MILLISECONDS)));
			writer.write("\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static long getMaxHeapSize() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		return memoryBean.getHeapMemoryUsage().getMax();
	}

	private static void writeGeneralInfoFile() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, "stats.txt")));

			writer.write("========================================\n");
			write(writer, "Soot Initialization", format(init.elapsed(MILLISECONDS)));
			write(writer, "Analysis duration", format(analysis.elapsed(MILLISECONDS)));
			write(writer, "IFDS duration", format(totalIfds()));
			write(writer, "Semi-Path creation", format(totalPathCreation()));
			write(writer, "Combined Paths", format(combinedPaths.elapsed(MILLISECONDS)));
			writer.write("========================================\n");
			write(writer, "IFDS Propagations", totalIFdsPropagations());
			write(writer, "Reports", reports.size());
			write(writer, "Semi-Paths", totalSemiPaths(reports.keySet()));
			write(writer, "Combined Paths", createdCombinedPaths.size());
			writer.write("========================================\n");

			writer.write("Soot Arguments:\n");
			for (String arg : sootArgs) {
				writer.write("\t");
				writer.write(arg);
				writer.write("\n");
			}

			for (AnalysisContext context : ifdsContextData.keySet()) {
				writer.write("========================================\n");
				IfdsData data = ifdsContextData.get(context);
				write(writer, "Solver", data.solver.getClass().getSimpleName());
				writer.write(context.toString());
				write(writer, "Duration", format(data.watch.elapsed(MILLISECONDS)));
				write(writer, "Propagations", data.propagations);
				write(writer, "Reports", Iterables.size(reports(context)));
				write(writer, "Semi-Paths", totalSemiPaths(reports(context)));
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeSemiPaths() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, "semi-paths.txt")));
			for (ReportData data : reports.values()) {
				for (Path path : data.paths) {
					writer.write(path.toContextAwareString());
					writer.write("\n\n=========================================\n\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeValidPaths() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, "combined-paths.txt")));
			for (Path path : createdCombinedPaths) {
				writer.write(path.toContextAwareString());
				writer.write("\n\n=========================================\n\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void write(BufferedWriter writer, String label, Object value) throws IOException {
		writer.write(String.format("%-25s %s\n", label + ":", String.valueOf(value)));
	}

	private static Iterable<Report> reports(final AnalysisContext context) {
		return Iterables.filter(reports.keySet(), new Predicate<Report>() {
			@Override
			public boolean apply(Report report) {
				return report.context == context;
			}
		});
	}

	private static int totalSemiPaths(Iterable<Report> _reports) {
		int result = 0;
		for (Report report : _reports) {
			ReportData data = reports.get(report);
			result += data.paths.size();
		}
		return result;
	}

	private static int totalIFdsPropagations() {
		int result = 0;
		for (IfdsData data : solverData.values()) {
			result += data.propagations;
		}
		return result;
	}

	private static long totalPathCreation() {
		long result = 0;
		for (ReportData data : reports.values()) {
			result += data.watch.elapsed(MILLISECONDS);
		}
		return result;
	}

	private static String format(long millis) {
		long h = MILLISECONDS.toHours(millis);
		millis -= HOURS.toMillis(h);

		long m = MILLISECONDS.toMinutes(millis);
		millis -= MINUTES.toMillis(m);

		long s = MILLISECONDS.toSeconds(millis);
		millis -= SECONDS.toMillis(s);

		return String.format("%02d:%02d:%02d,%03d", h, m, s, millis);
	}

	private static long totalIfds() {
		long result = 0;
		for (IfdsData data : solverData.values()) {
			result += data.watch.elapsed(MILLISECONDS);
		}
		return result;
	}

	private static class IfdsData {
		private Object solver;
		private Stopwatch watch = new Stopwatch();
		private int propagations;
	}

	private static class ReportData {
		private Collection<Path> paths;
		private Stopwatch watch = new Stopwatch();
	}
}
