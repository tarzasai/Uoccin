package net.ggelardi.uoccin.data;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class WeakList<E> extends AbstractList<E> {
	
	private ArrayList<WeakReference<E>> items;
	
	public WeakList() {
		items = new ArrayList<WeakReference<E>>();
	}
	
	public WeakList(Collection<E> c) {
		items = new ArrayList<WeakReference<E>>();
		addAll(0, c);
	}
	
	@Override
	public void add(int index, E element) {
		items.add(index, new WeakReference<E>(element));
	}
	
	@Override
	public Iterator<E> iterator() {
		return new WeakListIterator();
	}
	
	@Override
	public int size() {
		removeReleased();
		return items.size();
	}
	
	@Override
	public E get(int index) {
		return items.get(index).get();
	}
	
	//public E find(String )
	
	private void removeReleased() {
		for (WeakReference<E> weakReference : items) {
			WeakReference<E> ref = weakReference;
			if (ref.get() == null)
				items.remove(ref);
		}
	}
	
	private class WeakListIterator implements Iterator<E> {
		
		private int n;
		private int i;
		
		public WeakListIterator() {
			n = size();
			i = 0;
		}
		
		@Override
		public boolean hasNext() {
			return i < n;
		}
		
		@Override
		public E next() {
			return get(i++);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}