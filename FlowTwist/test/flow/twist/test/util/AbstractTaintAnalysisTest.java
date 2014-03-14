package flow.twist.test.util;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public abstract class AbstractTaintAnalysisTest extends AbstractAnalysisTest {

	@Rule
	public TestWatcher name = new TestWatcher() {
		@Override
		public void failed(Throwable e, Description description) {
			StringBuilder builder = new StringBuilder();
			builder.append("tmp/");
			builder.append(AbstractTaintAnalysisTest.this.getClass().getSimpleName());
			builder.append("_");
			builder.append(description.getMethodName());
			verifier.writeDebugFile(builder.toString());
		}
	};
	private AnalysisGraphVerifier verifier;

	protected AnalysisGraphVerifier runTest(final String... classNames) {
		runAnalysis(new TestAnalysis(classNames) {
			@Override
			protected void executeAnalysis() {
				verifier = executeTaintAnalysis(classNames);
			}
		});
		return verifier;
	}

	protected abstract AnalysisGraphVerifier executeTaintAnalysis(String[] classNames);

}
