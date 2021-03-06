package org.springframework.data.dozer.repository.support;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.dozer.repository.DozerRepository;
import org.springframework.data.dozer.repository.query.EscapeCharacter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.github.dozermapper.core.Mapper;

public class DozerRepositoryFactory extends RepositoryFactorySupport {
	protected final Mapper dozerMapper;
	protected final String conversionServiceName;
	protected final BeanFactory beanFactory;
	protected final MappingContext<?, ?> mappingContext;
	protected EntityPathResolver entityPathResolver;
	protected EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	private List<DozerRepositoryImplementation> repositoriesToValidateAfterRefresh = new ArrayList<DozerRepositoryImplementation>();

	/**
	 * Creates a new {@link DozerRepositoryFactory}.
	 * 
	 * @param dozerMapper must not be {@literal null}
	 */
	public DozerRepositoryFactory(Mapper dozerMapper, String conversionServiceName, BeanFactory beanFactory,
			MappingContext<?, ?> mappingContext) {

		this.dozerMapper = dozerMapper;
		this.conversionServiceName = conversionServiceName;
		this.entityPathResolver = SimpleEntityPathResolver.INSTANCE;
		this.beanFactory = beanFactory;
		this.mappingContext = mappingContext;

		addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {

			if (hasMethodReturningStream(repositoryInformation.getRepositoryInterface())) {
				factory.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
			}
		});
	}

	/**
	 * Configures the {@link EntityPathResolver} to be used. Defaults to
	 * {@link SimpleEntityPathResolver#INSTANCE}.
	 *
	 * @param entityPathResolver must not be {@literal null}.
	 */
	public void setEntityPathResolver(EntityPathResolver entityPathResolver) {

		Assert.notNull(entityPathResolver, "EntityPathResolver must not be null!");

		this.entityPathResolver = entityPathResolver;
	}

	/**
	 * Configures the escape character to be used for like-expressions created for
	 * derived queries.
	 *
	 * @param escapeCharacter a character used for escaping in certain like
	 *                        expressions.
	 */
	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport#
	 * getTargetRepository(org.springframework.data.repository.core.
	 * RepositoryMetadata)
	 */
	@Override
	protected final DozerRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information) {

		DozerRepositoryImplementation<?, ?> repository = getTargetRepository(information, dozerMapper,
				conversionServiceName, beanFactory);
		repository.setEscapeCharacter(escapeCharacter);

		return repository;
	}

	/**
	 * Callback to create a {@link DozerRepository} instance with the given
	 * {@link Mapper}
	 *
	 * @param information will never be {@literal null}.
	 * @param dozerMapper will never be {@literal null}.
	 * @return
	 */
	protected DozerRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information,
			Mapper dozerMapper, String conversionServiceName, BeanFactory beanFactory) {

		DozerEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
		Object repository = getTargetRepositoryViaReflection(information, information, entityInformation, dozerMapper,
				conversionServiceName, beanFactory);

		Assert.isInstanceOf(DozerRepositoryImplementation.class, repository);

		DozerRepositoryImplementation<?, ?> result = (DozerRepositoryImplementation<?, ?>) repository;
		repositoriesToValidateAfterRefresh.add(result);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport#
	 * getRepositoryBaseClass(org.springframework.data.repository.core.
	 * RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleDozerRepository.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport#
	 * getQueryLookupStrategy(org.springframework.data.repository.query.
	 * QueryLookupStrategy.Key,
	 * org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		// return Optional.of(DozerQueryLookupStrategy.create(entityManager, key,
		// extractor, evaluationContextProvider,
		// escapeCharacter));

		return Optional.empty();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport#
	 * getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> DozerEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		return (DozerEntityInformation<T, ID>) DozerEntityInformationSupport.getEntityInformation(domainClass,
				mappingContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport#
	 * getRepositoryFragments(org.springframework.data.repository.core.
	 * RepositoryMetadata)
	 */
	@Override
	protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryComposition.RepositoryFragments fragments = RepositoryComposition.RepositoryFragments.empty();

		boolean isQueryDslRepository = QUERY_DSL_PRESENT
				&& QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {
			// TODO:
		}

		return fragments;
	}

	private static boolean hasMethodReturningStream(Class<?> repositoryClass) {

		Method[] methods = ReflectionUtils.getAllDeclaredMethods(repositoryClass);

		for (Method method : methods) {
			if (Stream.class.isAssignableFrom(method.getReturnType())) {
				return true;
			}
		}

		return false;
	}

	public void validateAfterRefresh(ApplicationContext applicationContext) {
		List<MappingContext<?, ?>> arrayList = new ArrayList<MappingContext<?, ?>>();

		for (MappingContext<?, ?> context : BeanFactoryUtils
				.beansOfTypeIncludingAncestors(applicationContext, MappingContext.class).values()) {
			arrayList.add(context);
		}

		PersistentEntities persistenceEntites = new PersistentEntities(arrayList);

		for (DozerRepositoryImplementation<?, ?> repo : repositoriesToValidateAfterRefresh) {
			repo.validateAfterRefresh(persistenceEntites);
		}

		repositoriesToValidateAfterRefresh.clear();
	}

}
