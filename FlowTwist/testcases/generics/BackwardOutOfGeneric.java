package generics;

public class BackwardOutOfGeneric {

	public void string(String name, A<String> a) throws ClassNotFoundException {
		a.foo(name);
	}

	public void integer(Integer name, A<Integer> a) throws ClassNotFoundException {
		a.foo(name);
	}

	public void object(Object name, A<Object> a) throws ClassNotFoundException {
		a.foo(name);
	}

	public static interface A<T> {
		void foo(T t) throws ClassNotFoundException;
	}

	public static class B implements A<String> {

		@Override
		public void foo(String t) throws ClassNotFoundException {
			Class<?> c = Class.forName(t);
		}
	}
}
