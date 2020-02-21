package org.springframework.data.dozer.repository.support;

import org.springframework.data.dozer.repository.DozerRepository;
import org.springframework.data.dozer.repository.query.EscapeCharacter;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface DozerRepositoryImplementation<T, ID>
		extends DozerRepository<T, ID>/* , DozerSpecificationExecutor<T> */ {
	/**
	 * Configures the {@link EscapeCharacter} to be used with the repository.
	 *
	 * @param escapeCharacter Must not be {@literal null}.
	 */
	default void setEscapeCharacter(EscapeCharacter escapeCharacter) {

	}
	
	void validateAfterRefresh(PersistentEntities persistentEntities);
}
