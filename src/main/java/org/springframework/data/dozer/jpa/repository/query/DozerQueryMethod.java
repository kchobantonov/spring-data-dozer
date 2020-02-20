package org.springframework.data.dozer.jpa.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.dozer.repository.query.DefaultDozerEntityMetadata;
import org.springframework.data.dozer.repository.query.DozerEntityMetadata;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

public class DozerQueryMethod extends QueryMethod {
	private final Lazy<DozerEntityMetadata<?>> entityMetadata;

	public DozerQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
		Assert.notNull(method, "Method must not be null!");

		this.entityMetadata = Lazy.of(() -> new DefaultDozerEntityMetadata<>(getDomainClass()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.QueryMethod#getEntityInformation()
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DozerEntityMetadata<?> getEntityInformation() {
		return this.entityMetadata.get();
	}
}
