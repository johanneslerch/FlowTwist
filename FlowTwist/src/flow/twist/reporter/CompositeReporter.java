package flow.twist.reporter;

public class CompositeReporter implements IfdsReporter {

	private final IfdsReporter[] reporter;

	public CompositeReporter(IfdsReporter... reporter) {
		this.reporter = reporter;
	}

	@Override
	public void analysisFinished() {
		for (IfdsReporter r : reporter)
			r.analysisFinished();
	}

	@Override
	public void reportTrackable(Report report) {
		for (IfdsReporter r : reporter) {
			r.reportTrackable(report);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[CompositeReporter:\n");
		for (IfdsReporter r : reporter) {
			builder.append("\t");
			builder.append(r.toString().replaceAll("\n", "\n\t"));
			builder.append(",\n");
		}
		builder.append("]\n");
		return builder.toString();
	}
}
