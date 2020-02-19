package org.springframework.data.dozer.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.dozer.repository.query.DefaultDozerEntityMetadata;
import org.springframework.data.dozer.repository.query.DozerEntityMetadata;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

public abstract class DozerEntityInformationSupport<T, ID> extends AbstractEntityInformation<T, ID>
		implements DozerEntityInformation<T, ID> {
	private DozerEntityMetadata<T> metadata;

	/**
	 * Creates a new {@link DozerEntityInformationSupport} with the given domain
	 * class.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public DozerEntityInformationSupport(Class<T> domainClass) {
		super(domainClass);
		this.metadata = new DefaultDozerEntityMetadata<T>(domainClass);
	}

	/**
	 * Creates a {@link DozerEntityInformation} for the given domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> DozerEntityInformation<T, ?> getEntityInformation(Class<T> domainClass,
			MappingContext<?, ?> mappingContext) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new DozerPersistableEntityInformation(domainClass, mappingContext);
		} else {
			return new DozerMetamodelEntityInformation(domainClass, mappingContext);
		}
	}

	@Override
	public Class<?> getAdaptedJavaType() {
		return metadata.getAdaptedJavaType();
	}

	@Override
	public String getDozerMapId() {
		return metadata.getDozerMapId();
	}
}
