/*******************************************************************************
 * Copyright (c) 2009, 2012 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.core.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory data store for execution data. The data can be added through its
 * {@link IExecutionDataVisitor} interface. If execution data is provided
 * multiple times for the same class the data is merged, i.e. a probe is marked
 * as executed if it is reported as executed at least once. This allows to merge
 * coverage date from multiple runs. A instance of this class is not thread
 * safe.
 */
public final class ExecutionDataStore implements IExecutionDataVisitor {

	private final Map<Long, ExecutionData> entries = new HashMap<Long, ExecutionData>();

	/**
	 * Adds the given {@link ExecutionData} object into the store. If there is
	 * already execution data with this same class id, this structure is merged
	 * with the given one.
	 * 
	 * @param data
	 *            execution data to add or merge
	 * @throws IllegalStateException
	 *             if the given {@link ExecutionData} object is not compatible
	 *             to a corresponding one, that is already contained
	 * @see ExecutionData#assertCompatibility(long, String, int)
	 */
	public void put(final ExecutionData data) throws IllegalStateException {
		final Long id = Long.valueOf(data.getId());
		final ExecutionData entry = entries.get(id);
		if (entry == null) {
			entries.put(id, data);
		} else {
			entry.merge(data);
		}
	}

	/**
	 * Returns the {@link ExecutionData} entry with the given id if it exists in
	 * this store.
	 * 
	 * @param id
	 *            class id
	 * @return execution data or <code>null</code>
	 */
	public ExecutionData get(final long id) {
		return entries.get(Long.valueOf(id));
	}

	/**
	 * Returns the coverage data for the class with the given identifier. If
	 * there is no data available under the given id a new entry is created.
	 * 
	 * @param id
	 *            class identifier
	 * @param name
	 *            VM name of the class
	 * @param dataLength
	 *            probe data length
	 * @return execution data
	 */
	public ExecutionData get(final Long id, final String name,
			final int dataLength) {
		ExecutionData entry = entries.get(id);
		if (entry == null) {
			entry = new ExecutionData(id.longValue(), name, dataLength);
			entries.put(id, entry);
		} else {
			entry.assertCompatibility(id.longValue(), name, dataLength);
		}
		return entry;
	}

	/**
	 * Resets all execution data probes, i.e. marks them as not executed. The
	 * execution data objects itself are not removed.
	 */
	public void reset() {
		for (final ExecutionData executionData : this.entries.values()) {
			executionData.reset();
		}
	}

	/**
	 * Returns a collection that represents current contents of the store.
	 * 
	 * @return current contents
	 */
	public Collection<ExecutionData> getContents() {
		return entries.values();
	}

	/**
	 * Writes the content of the store to the given visitor interface.
	 * 
	 * @param visitor
	 *            interface to write content to
	 */
	public void accept(final IExecutionDataVisitor visitor) {
		for (final ExecutionData data : entries.values()) {
			visitor.visitClassExecution(data);
		}
	}

	// === IExecutionDataVisitor ===

	public void visitClassExecution(final ExecutionData data) {
		put(data);
	}
}
