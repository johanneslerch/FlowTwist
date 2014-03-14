package flow.twist.reporter;


public class ConsoleReporter implements IfdsReporter {

	@Override
	public void analysisFinished() {

	}

	@Override
	public void reportTrackable(Report report) {
		System.out.println("Reachable source " + report.targetUnit + " in method " + report.context.icfg.getMethodOf(report.targetUnit) + " by taint: "
				+ report.trackable);
	}

}
