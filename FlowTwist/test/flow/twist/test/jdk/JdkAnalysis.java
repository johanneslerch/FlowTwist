package flow.twist.test.jdk;

import static flow.twist.config.AnalysisConfigurationBuilder.forwardsFromAllParametersDefaults;
import static flow.twist.config.AnalysisConfigurationBuilder.i2oSimpleClassForNameDefaults;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import soot.G;

import com.google.common.collect.Lists;

import flow.twist.AbstractAnalysis;
import flow.twist.SolverFactory;
import flow.twist.config.AnalysisDirection;
import flow.twist.pathchecker.FilterSingleDirectionReports;
import flow.twist.reporter.ConsoleReporter;
import flow.twist.reporter.ResultForwardingReporter;
import flow.twist.states.StateMachineBasedPathReporter;
import flow.twist.transformer.PathToConsole;
import flow.twist.transformer.path.MergeEqualSelectorStrategy;
import flow.twist.transformer.path.PathBuilderResultTransformer;
import flow.twist.util.AnalysisUtil;

public class JdkAnalysis {

	@Test
	public void forwards() {
		new AbstractTestAnalysis() {
			@Override
			protected void executeAnalysis() {
				SolverFactory.runOneDirectionSolver(i2oSimpleClassForNameDefaults().direction(AnalysisDirection.FORWARDS).reporter(
						new ConsoleReporter()));
			}
		}.execute();
	}

	@Test
	public void backwards() {
		new AbstractTestAnalysis() {
			@Override
			protected void executeAnalysis() {
				SolverFactory.runOneDirectionSolver(i2oSimpleClassForNameDefaults().direction(AnalysisDirection.BACKWARDS).reporter(
						new ConsoleReporter()));
			}
		}.execute();
	}

	@Test
	public void innerToOuterWithPathsOnly() {
		new AbstractTestAnalysis() {
			@Override
			protected void executeAnalysis() {
				SolverFactory.runInnerToOuterSolver(i2oSimpleClassForNameDefaults().reporter(
						new ResultForwardingReporter(new FilterSingleDirectionReports(new PathBuilderResultTransformer(new PathToConsole(),
								new MergeEqualSelectorStrategy())))));
			}
		}.execute();
	}

	@Test
	@Ignore("Does not scale for JDK yet")
	public void innerToOuterWithStates() {
		new AbstractTestAnalysis() {
			@Override
			protected void executeAnalysis() {
				SolverFactory.runInnerToOuterSolver(i2oSimpleClassForNameDefaults().reporter(new StateMachineBasedPathReporter()));
			}
		}.execute();
	}

	@Test
	public void forwardsFromCallables() {
		new AbstractTestAnalysis() {
			@Override
			protected void executeAnalysis() {
				SolverFactory.runOneDirectionSolver(forwardsFromAllParametersDefaults(true).reporter(
						new ResultForwardingReporter(new PathBuilderResultTransformer(new PathToConsole(), new MergeEqualSelectorStrategy()))));
			}
		}.execute();
	}

	// @Test
	// public void biDirectional() {
	// new AbstractTestAnalysis() {
	// @Override
	// protected void executeAnalysis() {
	// SolverFactory.runBiDirectionSolver(new VulnerablePathReporter());
	// }
	// }.execute();
	// }

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

	private abstract static class AbstractTestAnalysis extends AbstractAnalysis {
		@Override
		protected ArrayList<String> createArgs() {
			String args = "-i java.lang. -allow-phantom-refs -f none -w -f none -p cg all-reachable:true -keep-line-number -pp";
			ArrayList<String> argList = Lists.newArrayList(args.split(" "));

			String javaHome = System.getProperty("java.home");
			String libDir = javaHome + File.separator + "lib" + File.separator;

			argList.add("-cp");
			argList.add(libDir + "jce.jar" + File.pathSeparator + libDir + "jsse.jar");

			argList.add("-process-dir");
			argList.add(libDir + "rt.jar");

			return argList;
		}
	}
}
