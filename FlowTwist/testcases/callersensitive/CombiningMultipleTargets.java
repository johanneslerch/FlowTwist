package callersensitive;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CombiningMultipleTargets {

	public Object foo(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Class<?> clazz = Class.forName(name);
		Object instance = instantiate(clazz);
		return instance;
	}

	private Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Constructor<?>[] constructors = clazz.getConstructors();
		Object instance = constructors[0].newInstance();
		return instance;
	}
}
