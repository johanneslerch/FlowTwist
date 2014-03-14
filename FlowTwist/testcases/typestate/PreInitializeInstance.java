package typestate;

public class PreInitializeInstance {

	private String name;

	public void set(String name) {
		this.name = name;
	}

	public Class<?> retrieve() throws ClassNotFoundException {
		return Class.forName(name);
	}
}
