package java.lang;

public class UseExistingSummary {

	public Class<?> foo(String name) throws ClassNotFoundException {
		Class<?> c = Class.forName(name);
		Class<?> d = bar(c);
		Class<?> e = bar(d);
		return e;
	}

	private Class<?> bar(Class<?> c) {
		return c;
	}
}
