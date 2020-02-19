package org.springframework.data.dozer.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Persistent;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Persistent
public @interface DozerEntity {
	/**
	 * <p>
	 * The adapted (backing) domain class for which a Spring repository of type
	 * {@code org.springframework.data.repository.PagingAndSortingRepository} should
	 * exist in the context.
	 * 
	 * <p>
	 * There should be also a Dozer mapping between the {@code DozerEntity}
	 * annotated class and the {@code adaptedDomainClass} as well as mapping for
	 * their ID classes
	 * </p>
	 * 
	 * @return the adapted domain class.
	 */
	Class<?> adaptedDomainClass();

	/**
	 * Dozer mapping context id to be used during mapping.
	 * 
	 * @return
	 */
	String dozerMapId() default "";

	/**
	 * Should we use spring conversion service for mapping when there is no explicit
	 * dozer mapping. The spring {@link Converter} should exist if this is true and
	 * no explicit dozer mapping exist.
	 * 
	 * @return true to use the conversion service when no explicit dozer mapping.
	 *         Either explicit dozer mapping or {@link Converter} must exist.
	 */
	boolean mapEntityUsingConvertionService() default false;

	/**
	 * Should we use the spring conversion service for mapping when there is no
	 * explicit dozer mapping. The spring {@link Converter} should exist if this is
	 * true and no explicit dozer mapping exist.
	 * 
	 * @return true to use the conversion service when no explicit dozer mapping.
	 *         Either explicit dozer mapping or {@link Converter} must exist.
	 */
	boolean mapEntityIdUsingConvertionService() default true;

}
