package generics;

public class ForwardThroughGeneric {

	public Class<?> foo(String name, A<Class<?>> a) throws ClassNotFoundException {
		Class<?> clazz = Class.forName(name);
		Class<?> result = a.id(clazz);
		return result;
	}

	public static interface A<T> {
		T id(T t);
	}

	public static class B implements A<Class<?>> {

		@Override
		public Class<?> id(Class<?> t) {
			return t;
		}
	}

	public static class C implements A<Integer> {

		@Override
		public Integer id(Integer t) {
			return t;
		}
	}
}
