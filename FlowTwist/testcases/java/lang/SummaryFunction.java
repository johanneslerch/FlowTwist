package java.lang;

public class SummaryFunction {

	public Class<?> foo(String name) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(name);
		Class<?> id = id(clazz);
		return id;
	}

	public Class<?> bar(String name) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(name);
		Class<?> id = id(clazz);
		return id;
	}

	private Class<?> id(Class<?> param) {
		Class<?> result = param;
		return result;
	}
}
