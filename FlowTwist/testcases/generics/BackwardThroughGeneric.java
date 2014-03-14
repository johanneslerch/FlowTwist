package generics;

import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class BackwardThroughGeneric {

	public Class<?> foo(String name, A<String> a) throws ClassNotFoundException {
		String idName = a.id(name);
		return Class.forName(idName);
	}

	public static interface A<T> {
		T id(T t);
	}

	public static class B implements A<String> {

		@Override
		public String id(String t) {
			return "CONSTANT";
		}
	}

	public static class C implements A<Integer> {

		@Override
		public Integer id(Integer t) {
			return t;
		}
	}
}
