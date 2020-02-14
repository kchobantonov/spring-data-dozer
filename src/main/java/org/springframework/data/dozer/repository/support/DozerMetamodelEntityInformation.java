package org.springframework.data.dozer.repository.support;

import org.springframework.data.dozer.mapping.DozerPersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

public class DozerMetamodelEntityInformation<T, ID> extends DozerEntityInformationSupport<T, ID> {
	private DozerPersistentEntity<T> persistentEntity;

	/**
	 * Creates a new {@link DozerMetamodelEntityInformation} for the given domain
	 * class/
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public DozerMetamodelEntityInformation(Class<T> domainClass, MappingContext<?, ?> mappingContext) {

		super(domainClass);
		persistentEntity = (DozerPersistentEntity<T>) mappingContext.getPersistentEntity(domainClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.EntityInformation#getId(java.lang.
	 * Object)
	 */
	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {
		return (ID) persistentEntity.getIdentifierAccessor(entity).getIdentifier();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) persistentEntity.getRequiredIdProperty().getActualType();
	}

}
