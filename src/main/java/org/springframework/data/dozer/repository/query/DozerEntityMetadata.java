package org.springframework.data.dozer.repository.query;

import org.springframework.core.convert.converter.Converter;
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

	/**
	 * Should we use spring conversion service for mapping when there is no explicit
	 * dozer mapping. The spring {@link Converter} should exist if this is true and
	 * no explicit dozer mapping exist.
	 * 
	 * @return true to use the conversion service when no explicit dozer mapping.
	 *         Either explicit dozer mapping or {@link Converter} must exist.
	 */
	boolean getMapEntityUsingConvertionService();

	/**
	 * Should we use the spring conversion service for mapping when there is no
	 * explicit dozer mapping. The spring {@link Converter} should exist if this is
	 * true and no explicit dozer mapping exist.
	 * 
	 * @return true to use the conversion service when no explicit dozer mapping.
	 *         Either explicit dozer mapping or {@link Converter} must exist.
	 */
	boolean getMapEntityIdUsingConvertionService();
}
