package flow.twist.reporter;

import java.util.List;

import com.google.common.collect.Lists;

import flow.twist.transformer.ResultTransformer;

public class ResultForwardingReporter implements IfdsReporter {

	private ResultTransformer<Iterable<Report>, ?> transformer;
	private List<Report> reports = Lists.newLinkedList();

	public ResultForwardingReporter(ResultTransformer<Iterable<Report>, ?> transformer) {
		this.transformer = transformer;
	}

	@Override
	public void analysisFinished() {
		transformer.transform(reports);
	}

	@Override
	public void reportTrackable(Report report) {
		reports.add(report);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "\n\t-> " + transformer.toString();
	}
}
