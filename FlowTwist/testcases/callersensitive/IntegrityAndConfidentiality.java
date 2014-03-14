package callersensitive;

import sun.reflect.CallerSensitive;

@SuppressWarnings("restriction")
public class IntegrityAndConfidentiality {

	@CallerSensitive
	public static Object dangerous(Object o) {
		return new Object();
	}

	public static Object foo(Object o) {
		return dangerous(o);
	}
}
