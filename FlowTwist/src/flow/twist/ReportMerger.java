package flow.twist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ReportMerger {

	public static void main(String[] args) throws IOException {
		TreeMap<String, TreeMap<File, Integer>> results = Maps.newTreeMap();
		TreeSet<File> jres = Sets.newTreeSet();

		File resultsDir = new File("results");
		for (File jreDir : resultsDir.listFiles()) {
			jres.add(jreDir);
			File reportFile = new File(jreDir, "report.csv");
			if (!reportFile.exists()) {
				System.out.println("No report file for " + jreDir);
				continue;
			}

			BufferedReader reader = new BufferedReader(new FileReader(reportFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(";");
				TreeMap<File, Integer> innerMap = results.containsKey(split[0]) ? results.get(split[0]) : Maps.<File, Integer> newTreeMap();
				innerMap.put(jreDir, Integer.parseInt(split[1]));
				results.put(split[0], innerMap);
			}
			reader.close();
		}

		FileWriter writer = new FileWriter("report.csv");
		writer.write(";");// no sink
		for (File jre : jres) {
			writer.write(jre.getName());
			writer.write(";");
		}
		writer.write("\n");

		for (String sink : results.keySet()) {
			writer.write(sink);
			writer.write(";");

			TreeMap<File, Integer> inner = results.get(sink);
			for (File jre : jres) {
				if (inner.containsKey(jre))
					writer.write(String.valueOf(inner.get(jre)));
				writer.write(";");
			}
			writer.write("\n");
		}
		writer.close();
	}
}
