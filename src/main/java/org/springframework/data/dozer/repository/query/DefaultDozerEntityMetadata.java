package org.springframework.data.dozer.repository.query;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.util.Assert;

public class DefaultDozerEntityMetadata<T> implements DozerEntityMetadata<T> {
	private final Class<T> domainType;

	/**
	 * Creates a new {@link DefaultDozerEntityMetadata} for the given domain type.
	 *
	 * @param domainType must not be {@literal null}.
	 */
	public DefaultDozerEntityMetadata(Class<T> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");
		this.domainType = domainType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
	 */
	@Override
	public Class<T> getJavaType() {
		return domainType;
	}

	@Override
	public Class<?> getAdaptedJavaType() {
		DozerEntity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, DozerEntity.class);
		return entity.adaptedDomainClass();
	}

	@Override
	public Class<?> getAdaptedRepositoryJavaType() {
		DozerEntity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, DozerEntity.class);
		return entity.adaptedRepositoryClass();
	}

	@Override
	public String getDozerMapId() {
		DozerEntity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, DozerEntity.class);
		return entity.dozerMapId();
	}

	@Override
	public boolean getMapEntityUsingConvertionService() {
		DozerEntity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, DozerEntity.class);
		return entity.mapEntityUsingConvertionService();
	}

	@Override
	public boolean getMapEntityIdUsingConvertionService() {
		DozerEntity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, DozerEntity.class);
		return entity.mapEntityIdUsingConvertionService();
	}
}
