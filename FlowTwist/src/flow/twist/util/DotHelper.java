package flow.twist.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import att.grappa.Graph;

public class DotHelper {

	public static void writeFilesForGraph(String filename, Graph graph) {
		try {
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			graph.printGraph(byteOutStream);
			String output = byteOutStream.toString().replaceAll("#", "\"");
			// System.out.println(output);

			File file = new File(filename + ".dot").getAbsoluteFile();
			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();

			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(output);
			fileWriter.close();

			String dot = System.getProperty("dot.executable", "C:\\Program Files (x86)\\Graphviz\\bin\\dot.exe");
			if (!new File(dot).exists()) {
				dot = "/usr/local/bin/dot";
			}
			if (!new File(dot).exists()) {
				dot = "/usr/bin/dot";
			}
			if (new File(dot).exists()) {
				Process pngProcess = Runtime.getRuntime().exec(new String[] { dot, "-Tpng", file.getAbsolutePath(), "-o", filename + ".png" });
				Process pdfProcess = Runtime.getRuntime().exec(new String[] { dot, "-Tpdf", file.getAbsolutePath(), "-o", filename + ".pdf" });
				pngProcess.waitFor();
				pdfProcess.waitFor();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
