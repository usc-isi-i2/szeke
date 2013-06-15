package edu.isi.karma.util;

import java.util.Enumeration;
import java.util.Iterator;

public class Iterator2Enumeration<E> implements Enumeration<E> {

	private Iterator<E> iterator;
	
	public Iterator2Enumeration(Iterator<E> iter) {
		this.iterator = iter;
	}
	
	@Override
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}
	
	@Override
	public E nextElement() {
		return iterator.next();
	}
}
