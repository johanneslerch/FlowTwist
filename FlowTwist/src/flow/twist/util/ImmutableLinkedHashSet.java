package flow.twist.util;

import java.util.Iterator;

import fj.F;
import fj.data.List;

public class ImmutableLinkedHashSet<T> extends ImmutableHashSet<T> implements Iterable<T> {

	public static <T> ImmutableLinkedHashSet<T> empty() {
		ImmutableLinkedHashSet<T> result = new ImmutableLinkedHashSet<T>();
		result.initEmpty();
		return result;
	}

	private List<T> linkedItems;

	protected ImmutableLinkedHashSet() {
	}

	@Override
	protected ImmutableLinkedHashSet<T> create() {
		return new ImmutableLinkedHashSet<T>();
	}

	@Override
	protected void initEmpty() {
		super.initEmpty();
		linkedItems = List.nil();
	}

	@Override
	public ImmutableLinkedHashSet<T> add(T item) {
		ImmutableLinkedHashSet<T> result = (ImmutableLinkedHashSet<T>) super.add(item);
		result.linkedItems = linkedItems.cons(item);
		return result;
	}

	@Override
	public Iterator<T> iterator() {
		return linkedItems.iterator();
	}

	public T head() {
		return linkedItems.head();
	}

	@Override
	public ImmutableHashSet<T> remove(final T item) {
		ImmutableLinkedHashSet<T> result = (ImmutableLinkedHashSet<T>) super.remove(item);
		result.linkedItems = linkedItems.filter(new F<T, Boolean>() {
			@Override
			public Boolean f(T other) {
				return isEquals(other, item);
			}
		});
		return result;
	}

	public ImmutableLinkedHashSet<T> removeHead() {
		ImmutableLinkedHashSet<T> result = (ImmutableLinkedHashSet<T>) super.remove(linkedItems.head());
		result.linkedItems = linkedItems.tail();
		return result;
	}

	public fj.data.List<T> asList() {
		return linkedItems;
	}

	public int size() {
		return linkedItems.length();
	}
}
