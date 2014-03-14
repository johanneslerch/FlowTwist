package flow.twist.util;

import fj.F;
import fj.Ord;
import fj.Ordering;
import fj.data.List;
import fj.data.Option;
import fj.data.TreeMap;

public class ImmutableHashSet<T> {

	public static <T> ImmutableHashSet<T> empty() {
		ImmutableHashSet<T> result = new ImmutableHashSet<T>();
		result.initEmpty();
		return result;
	}

	private TreeMap<T, List<T>> backingMap;

	protected ImmutableHashSet() {

	}

	protected ImmutableHashSet<T> create() {
		return new ImmutableHashSet<>();
	}

	protected void initEmpty() {
		Ord<T> ord = Ord.ord(new F<T, F<T, Ordering>>() {
			@Override
			public F<T, Ordering> f(final T first) {
				return new F<T, Ordering>() {
					@Override
					public Ordering f(T second) {
						return Ord.intOrd.compare(getHashCode(first), getHashCode(second));
					}
				};
			}
		});
		backingMap = TreeMap.empty(ord);
	}

	protected boolean isEquals(T t, T item) {
		return t.equals(item);
	}

	protected int getHashCode(T item) {
		return item.hashCode();
	}

	public ImmutableHashSet<T> add(final T item) {
		ImmutableHashSet<T> result = create();
		Option<List<T>> existingBucket = backingMap.get(item);
		List<T> newBucket;
		if (existingBucket.isSome()) {
			newBucket = existingBucket.some().filter(new F<T, Boolean>() {
				@Override
				public Boolean f(T existingItem) {
					return !isEquals(existingItem, item);
				}
			}).cons(item);
		} else {
			newBucket = List.single(item);
		}
		result.backingMap = backingMap.set(item, newBucket);
		return result;
	}

	public boolean contains(final T item) {
		Option<List<T>> bucket = backingMap.get(item);
		if (bucket.isSome()) {
			return bucket.some().exists(new F<T, Boolean>() {
				@Override
				public Boolean f(T existingItem) {
					return isEquals(existingItem, item);
				}
			});
		}
		return false;
	}

	public ImmutableHashSet<T> remove(final T item) {
		ImmutableHashSet<T> result = create();
		Option<List<T>> existingBucket = backingMap.get(item);
		List<T> newBucket;
		if (existingBucket.isSome()) {
			newBucket = existingBucket.some().filter(new F<T, Boolean>() {
				@Override
				public Boolean f(T existingItem) {
					return !isEquals(existingItem, item);
				}
			});
			if (newBucket.isEmpty()) {
				result.backingMap = backingMap.delete(item);
			} else {
				result.backingMap = backingMap.set(item, newBucket);
			}
		} else {
			newBucket = List.single(item);
			result.backingMap = backingMap.set(item, newBucket);
		}
		return result;
	}

	public boolean isEmpty() {
		return backingMap.isEmpty();
	}
}
