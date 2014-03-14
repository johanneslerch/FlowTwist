package type;

public class TypeConversion {

	public static Class<?> local(Integer param) throws ClassNotFoundException {
		Object x = param;
		return Class.forName((String) x);
	}

	public static Class<?> arrayItem(Integer param) throws ClassNotFoundException {
		Object[] x = new Object[] { param };
		return Class.forName((String) x[0]);
	}

	public static Class<?> array(Integer param) throws ClassNotFoundException {
		Object[] x = new Integer[] { param };
		return Class.forName((String) x[0]);
	}
}
