package flow.twist.reporter;


public interface IfdsReporter {

	void analysisFinished();

	void reportTrackable(Report report);
}
