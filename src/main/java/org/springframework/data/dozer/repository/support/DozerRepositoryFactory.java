package org.springframework.data.dozer.repository.support;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.dozer.repository.DozerRepository;
import org.springframework.data.dozer.repository.query.EscapeCharacter;
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
	private final Mapper dozerMapper;
	private final BeanFactory beanFactory;

	private EntityPathResolver entityPathResolver;
	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	private List<DozerRepositoryImplementation> repositoriesToValidateAfterRefresh = new ArrayList<DozerRepositoryImplementation>();

	/**
	 * Creates a new {@link DozerRepositoryFactory}.
	 * 
	 * @param dozerMapper must not be {@literal null}
	 */
	public DozerRepositoryFactory(Mapper dozerMapper, BeanFactory beanFactory) {
		this.entityPathResolver = SimpleEntityPathResolver.INSTANCE;
		this.dozerMapper = dozerMapper;
		this.beanFactory = beanFactory;

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

		DozerRepositoryImplementation<?, ?> repository = getTargetRepository(information, dozerMapper, beanFactory);
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
			Mapper dozerMapper, BeanFactory beanFactory) {

		DozerEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
		Object repository = getTargetRepositoryViaReflection(information, entityInformation, dozerMapper, beanFactory);

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
		/*
		 * return Optional .of(DozerQueryLookupStrategy.create(entityManager, key,
		 * extractor, evaluationContextProvider, escapeCharacter));
		 */

		return Optional.<QueryLookupStrategy>empty();
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

		return (DozerEntityInformation<T, ID>) DozerEntityInformationSupport.getEntityInformation(domainClass);
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
			//TODO:
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

	public void validateAfterRefresh() {
		for (DozerRepositoryImplementation<?, ?> repo : repositoriesToValidateAfterRefresh) {
			repo.validateAfterRefresh();
		}

		repositoriesToValidateAfterRefresh.clear();
	}

}
