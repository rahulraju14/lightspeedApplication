/**
 * Filter.java
 */
package com.lightspeedeps.util.common;


/**
 * An interface used to provide filtering of arbitrary object types. A method
 * (e.g., "doSomething()", see below) that processes a collection of
 * Objects can allow its caller to restrict the processing to any arbitrary
 * subset of the collection by having the caller provide a Filter instance. The
 * doSomething() method will then invoke "f.filter()" for each Object in the
 * collection, and only process those objects for which the filter method
 * returns true. The outline of "doSomething()" might look like this:
 *<pre>
 *	public void doSomething(Collection< Foo > collection, Filter< Foo > f) {
		//...
		for (Foo foo : collection) {
			if (f.filter(foo)) {
				// ... process instance 'foo'
			}
			// presumably, if f.filter(foo) returns false, 'foo' is not processed
		}
		//...
	}
	</pre>
 */
public interface Filter<T> {

	/**
	 * Determine if the provided object matches the criteria defined within this
	 * Filter instance.
	 *
	 * @param filtered The object to be tested.
	 * @return True iff the 'filtered' object instance matches the criteria of
	 *         this Filter instance.
	 */
	public boolean filter(T filtered);

}
