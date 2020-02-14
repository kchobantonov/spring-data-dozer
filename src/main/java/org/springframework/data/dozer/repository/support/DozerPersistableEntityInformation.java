package org.springframework.data.dozer.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

public class DozerPersistableEntityInformation<T extends Persistable<ID>, ID>
		extends DozerMetamodelEntityInformation<T, ID> {

	/**
	 * Creates a new {@link DozerPersistableEntityInformation} for the given domain
	 * class.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public DozerPersistableEntityInformation(Class<T> domainClass, MappingContext<?, ?> mappingContext) {
		super(domainClass, mappingContext);
	}

	@Override
	public boolean isNew(T entity) {
		return entity.isNew();
	}

	@Nullable
	@Override
	public ID getId(T entity) {
		return entity.getId();
	}

}
