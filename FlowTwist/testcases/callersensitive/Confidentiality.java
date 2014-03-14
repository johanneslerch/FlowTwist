package callersensitive;

import sun.reflect.CallerSensitive;

@SuppressWarnings("restriction")
public class Confidentiality {

	@CallerSensitive
	public static Object dangerous() {
		return new Object();
	}

	public static Object foo() {
		return dangerous();
	}
}
