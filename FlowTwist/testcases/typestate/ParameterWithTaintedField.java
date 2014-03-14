package typestate;

public class ParameterWithTaintedField {

	public static class DataStruct {
		String className;
	}

	public static Class<?> foo(DataStruct ds) throws ClassNotFoundException {
		return Class.forName(ds.className);
	}
}
