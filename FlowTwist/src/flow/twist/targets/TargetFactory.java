package flow.twist.targets;

import java.util.ArrayList;
import java.util.List;

public class TargetFactory {
	public static Iterable<AnalysisTarget> createTargets() {
		List<AnalysisTarget> result = new ArrayList<AnalysisTarget>();

		result.add(new SimpleClassForNameTarget());
		// result.add(new ExtendedClassForNameTarget());
		// result.add(new PackageAccessCheckTarget());
		// result.add(new GenericCallerSensitive());

		return result;
	}
}
