package callersensitive;

import sun.reflect.CallerSensitive;

@SuppressWarnings("restriction")
public class Integrity {

	@CallerSensitive
	public static void dangerous(Object o) {
	}

	public static void foo(Object o) {
		dangerous(o);
	}
}
