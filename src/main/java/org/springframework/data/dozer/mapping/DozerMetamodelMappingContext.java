package org.springframework.data.dozer.mapping;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

public class DozerMetamodelMappingContext
		extends AbstractMappingContext<DozerPersistentEntityImpl<?>, DozerPersistentProperty> {

	/**
	 * Creates a new Dozer based {@link MappingContext}.
	 *
	 */
	public DozerMetamodelMappingContext() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> DozerPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new DozerPersistentEntityImpl<T>(typeInformation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * createPersistentProperty(java.lang.reflect.Field,
	 * java.beans.PropertyDescriptor,
	 * org.springframework.data.mapping.model.MutablePersistentEntity,
	 * org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected DozerPersistentProperty createPersistentProperty(Property property, DozerPersistentEntityImpl<?> owner,
			SimpleTypeHolder simpleTypeHolder) {
		return new DozerPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * shouldCreatePersistentEntityFor(org.springframework.data.util.
	 * TypeInformation)
	 */
	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return hasDozerEntityAnnotation(type.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#
	 * hasPersistentEntityFor(java.lang.Class)
	 */
	@Override
	public boolean hasPersistentEntityFor(Class<?> type) {
		return super.hasPersistentEntityFor(type) || hasDozerEntityAnnotation(type);
	}

	private boolean hasDozerEntityAnnotation(Class<?> type) {
		return AnnotatedElementUtils.findMergedAnnotation(type, DozerEntity.class) != null;
	}
}
