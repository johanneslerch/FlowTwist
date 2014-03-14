package java.lang;

public class MultipleClasses {
	
	public Class<?> leakingMethod(String name) throws ClassNotFoundException {
		return new HiddenClass().uncheckedMethod(name);
	}

}