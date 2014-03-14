package typestate;

public class ThreePhasesRetrieval {

	private String name;
	private Class<?> clazz;

	public void set(String name) {
		this.name = name;
	}

	public void load() throws ClassNotFoundException {
		clazz = Class.forName(name);
	}

	public Class<?> retrieve() {
		return clazz;
	}
}
