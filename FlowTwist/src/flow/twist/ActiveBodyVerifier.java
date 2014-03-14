package flow.twist;

import java.util.HashSet;

import soot.SootMethod;

import com.google.common.collect.Sets;

public class ActiveBodyVerifier {

	private static HashSet<SootMethod> active = Sets.newHashSet();
	private static HashSet<SootMethod> inactive = Sets.newHashSet();

	public static void markActive(SootMethod m) {
		active.add(m);
	}

	public static void markInactive(SootMethod m) {
		inactive.add(m);
	}

	public static void assertActive(SootMethod m) {
		if (!active.contains(m))
			throw new IllegalStateException(m + " was assumed to be active, but was not. Known to be inactive: " + inactive.contains(m));
	}

}
