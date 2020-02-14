package org.springframework.data.dozer.mapping;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

public class DozerPersistentPropertyImpl extends AnnotationBasedPersistentProperty<DozerPersistentProperty>
		implements DozerPersistentProperty {

	public DozerPersistentPropertyImpl(Property property, PersistentEntity<?, DozerPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}

	@Override
	protected Association<DozerPersistentProperty> createAssociation() {
		return new Association<DozerPersistentProperty>(this, null);
	}

}
