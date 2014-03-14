package type;

public class StringBuilderTest {

	public Class<?> leakStringBuilder(StringBuilder a) throws ClassNotFoundException {
		return Class.forName(a.toString());
	}

	public Class<?> throughStringBuilder(String a) throws ClassNotFoundException {
		StringBuilder b = new StringBuilder(a);
		return Class.forName(b.toString());
	}

	public Class<?> trackStringBuilder(String b) throws ClassNotFoundException {
		String a = b;
		StringBuilder builder = new StringBuilder(a);
		StringBuilder alias = builder;
		return Class.forName(alias.toString());
	}
}
