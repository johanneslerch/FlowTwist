package flow.twist.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;

import soot.G;
import flow.twist.AbstractAnalysis;
import flow.twist.util.AnalysisUtil;

public abstract class AbstractAnalysisTest {

	protected void runAnalysis(TestAnalysis analysis) {
		analysis.execute();
	}

	@Before
	public void setUp() throws Exception {
		String jrePath = System.getProperty("java.home");
		jrePath = jrePath.substring(0, jrePath.lastIndexOf(File.separator));
		AnalysisUtil.initRestrictedPackages(jrePath);
	}

	@After
	public void tearDown() throws Exception {
		try {
		} finally {
			G.reset();
		}
	}

	public static abstract class TestAnalysis extends AbstractAnalysis {

		private String[] classNames;

		public TestAnalysis(String... classNames) {
			this.classNames = classNames;
		}

		@Override
		protected ArrayList<String> createArgs() {
			String classPath = "bin";
			String ARGS = "-no-bodies-for-excluded -p jb use-original-names:true -f none -cp " + classPath
					+ " -pp -w -p cg all-reachable:true -keep-line-number ";

			ArrayList<String> argList = new ArrayList<String>(Arrays.asList(ARGS.split(" ")));

			for (String className : classNames) {
				argList.add("-i");
				argList.add(className);
			}
			for (String className : classNames) {
				// argument class
				argList.add(className);
			}
			return argList;
		}
	}
}
