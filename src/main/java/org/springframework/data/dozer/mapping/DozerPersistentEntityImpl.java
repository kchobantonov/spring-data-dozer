package org.springframework.data.dozer.mapping;

import java.util.Comparator;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

public class DozerPersistentEntityImpl<T> extends BasicPersistentEntity<T, DozerPersistentProperty>
		implements DozerPersistentEntity<T> {

	/**
	 * Creates a new {@link DozerPersistentEntityImpl} using the given
	 * {@link TypeInformation} and {@link Comparator}.
	 *
	 * @param information must not be {@literal null}.
	 */
	public DozerPersistentEntityImpl(TypeInformation<T> information) {

		super(information, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#
	 * returnPropertyIfBetterIdPropertyCandidateOrNull(org.springframework.data.
	 * mapping.PersistentProperty)
	 */
	@Override
	protected DozerPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			DozerPersistentProperty property) {
		return property.isIdProperty() ? property : null;
	}

}
