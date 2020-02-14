package org.springframework.data.dozer.repository.support;

import org.springframework.lang.Nullable;

public class DozerMetamodelEntityInformation<T, ID> extends DozerEntityInformationSupport<T, ID> {

	/**
	 * Creates a new {@link DozerMetamodelEntityInformation} for the given domain
	 * class/
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public DozerMetamodelEntityInformation(Class<T> domainClass) {

		super(domainClass);
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
		return (ID) getEntityInformation(getJavaType()).getId(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) getEntityInformation(getJavaType()).getIdType();
	}

}
