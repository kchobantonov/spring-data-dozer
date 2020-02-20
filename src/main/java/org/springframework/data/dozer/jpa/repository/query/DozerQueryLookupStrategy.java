package org.springframework.data.dozer.jpa.repository.query;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import com.github.dozermapper.core.Mapper;

public class DozerQueryLookupStrategy implements QueryLookupStrategy {

	private final Mapper dozerMapper;
	private final QueryLookupStrategy adaptedQueryLookupStrategy;
	private final String conversionServiceName;
	private final BeanFactory beanFactory;

	public DozerQueryLookupStrategy(Mapper dozerMapper, String conversionServiceName, BeanFactory beanFactory,
			QueryLookupStrategy adaptedQueryLookupStrategy) {
		this.dozerMapper = dozerMapper;
		this.adaptedQueryLookupStrategy = adaptedQueryLookupStrategy;
		this.conversionServiceName = conversionServiceName;
		this.beanFactory = beanFactory;
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			NamedQueries namedQueries) {
		return new DozerRepositoryQuery(new DozerQueryMethod(method, metadata, factory), dozerMapper,
				adaptedQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries), conversionServiceName,
				beanFactory);
	}
}
