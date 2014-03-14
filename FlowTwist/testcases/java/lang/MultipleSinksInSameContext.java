package java.lang;

public class MultipleSinksInSameContext {

	public static boolean random;

	public static Class<?> bar(String name) throws ClassNotFoundException {
		return foo(name);
	}

	private static Class<?> foo(String name) throws ClassNotFoundException {
		if (random) {
			Class<?> c1 = Class.forName(name);
			return c1;
		} else {
			Class<?> c2 = Class.forName(name);
			return c2;
		}
	}
}
