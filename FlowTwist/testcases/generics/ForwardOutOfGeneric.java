package generics;

public class ForwardOutOfGeneric {

	public Class<?> clazz(A<Class<?>> a) throws ClassNotFoundException {
		return a.foo();
	}

	public Integer integer(A<Integer> a) throws ClassNotFoundException {
		return a.foo();
	}

	public Object object(A<Object> a) throws ClassNotFoundException {
		return a.foo();
	}

	public static interface A<T> {
		T foo() throws ClassNotFoundException;
	}

	public static class B implements A<Class<?>> {

		private String className;

		@Override
		public Class<?> foo() throws ClassNotFoundException {
			return Class.forName(className);
		}
	}
}
