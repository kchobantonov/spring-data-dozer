package org.springframework.data.dozer.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.stereotype.Repository;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repository
public @interface DozerRepository {

	/**
	 * <p>
	 * The adapted (backing) repository class. This is useful in case of multiple
	 * repositories for the same domain class. The default is to expect a single
	 * backing repository per adapted domain class.
	 * 
	 * @return the adapted repository class.
	 */
	Class<?> adaptedRepositoryClass();

}
