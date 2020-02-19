package org.springframework.data.dozer.repository.query;

import org.springframework.data.repository.core.EntityMetadata;

public interface DozerEntityMetadata<T> extends EntityMetadata<T> {

	/**
	 * Returns the actual target domain class type.
	 *
	 * @return
	 */
	Class<?> getAdaptedJavaType();

	/**
	 * Dozer mapping context id.
	 * 
	 * @return
	 */
	String getDozerMapId();
}
