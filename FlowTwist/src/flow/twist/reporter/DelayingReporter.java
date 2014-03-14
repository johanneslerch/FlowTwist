package flow.twist.reporter;

import java.util.Set;

import com.google.common.collect.Sets;

public class DelayingReporter implements IfdsReporter {

	private IfdsReporter decoratee;
	private Set<Report> reports;

	public DelayingReporter(IfdsReporter decoratee) {
		this.decoratee = decoratee;
		reports = Sets.newHashSet();
	}

	@Override
	public synchronized void reportTrackable(Report report) {
		reports.add(report);
	}

	@Override
	public void analysisFinished() {
		System.out.println("Flushing " + reports.size() + " reports that were delayed.");
		int i = 0;
		for (Report report : reports) {
			decoratee.reportTrackable(report);
		}
		decoratee.analysisFinished();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "\n\t-> " + decoratee.toString().replaceAll("\n", "\n\t");
	}
}
