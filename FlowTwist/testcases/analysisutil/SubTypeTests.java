package analysisutil;

public class SubTypeTests {

	public static class A {

	}

	public static class B extends A {
		public String helloWorld() {
			return "hello";
		}
	}

	public static interface I {

	}

	public static class J implements I {

	}
}
