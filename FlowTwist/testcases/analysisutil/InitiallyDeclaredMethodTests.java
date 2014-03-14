package analysisutil;

public class InitiallyDeclaredMethodTests {

	public static interface I {
		public void inInterface();
	}

	public static interface J extends I {
	}

	public static class A implements I {

		public void inInterface() {

		}

		public void inSuperType() {

		}
	}

	public static class B extends A {
		@Override
		public void inInterface() {
			super.inInterface();
		}

		@Override
		public void inSuperType() {
			super.inSuperType();
		}

		public void inCurrentType() {

		}
	}

	public static class C implements J {
		@Override
		public void inInterface() {

		}
	}

	public static class D extends B {

	}

	public static class E extends D {
		@Override
		public void inSuperType() {
			super.inSuperType();
		}
	}
}
