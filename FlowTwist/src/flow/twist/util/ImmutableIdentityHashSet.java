package flow.twist.util;

public class ImmutableIdentityHashSet<T> extends ImmutableHashSet<T> {

	public static <T> ImmutableIdentityHashSet<T> empty() {
		ImmutableIdentityHashSet<T> result = new ImmutableIdentityHashSet<T>();
		result.initEmpty();
		return result;
	}

	@Override
	protected ImmutableIdentityHashSet<T> create() {
		return new ImmutableIdentityHashSet<T>();
	}

	protected ImmutableIdentityHashSet() {
		super();
	}

	protected boolean isEquals(T t, T item) {
		return t == item;
	}

	protected int getHashCode(T item) {
		return System.identityHashCode(item);
	}

	@Override
	public ImmutableIdentityHashSet<T> add(T item) {
		return (ImmutableIdentityHashSet<T>) super.add(item);
	}

	@Override
	public ImmutableIdentityHashSet<T> remove(T item) {
		return (ImmutableIdentityHashSet<T>) super.remove(item);
	}
}
