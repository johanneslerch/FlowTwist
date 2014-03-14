package typestate;

public class PreInitializeStatic {

	private static String name;

	public static void set(String _name) {
		name = _name;
	}

	public static Class<?> retrieve() throws ClassNotFoundException {
		return Class.forName(name);
	}
}
