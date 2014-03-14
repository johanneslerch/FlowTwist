package java.lang;

import sun.reflect.Reflection;

public class CallerClassLoader {

	public Class<?> okMethod(String name) throws ClassNotFoundException {
		return Class.forName(name, true, ClassLoader.getClassLoader(Reflection.getCallerClass()));
	}

	public Class<?> problematicMethod(String name) throws ClassNotFoundException {
		return Class.forName(name, true, ClassLoader.getClassLoader(Reflection.getCallerClass()));
	}

	public Class<?> leakingMethod(String name) throws ClassNotFoundException {
		return problematicMethod(name);
	}

}
