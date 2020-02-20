package org.springframework.data.dozer.jpa.repository.support;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.dozer.jpa.repository.query.DozerQueryLookupStrategy;
import org.springframework.data.dozer.repository.support.DozerRepositoryFactory;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.github.dozermapper.core.Mapper;

import lombok.extern.slf4j.Slf4j;

public class DozerJpaRepositoryFactory extends DozerRepositoryFactory {
	private final EntityManager entityManager;
	private final QueryExtractor extractor;

	public DozerJpaRepositoryFactory(EntityManager entityManager, Mapper dozerMapper, String conversionServiceName,
			BeanFactory beanFactory, MappingContext<?, ?> mappingContext) {
		super(dozerMapper, conversionServiceName, beanFactory, mappingContext);

		Assert.notNull(entityManager, "EntityManager must not be null!");

		this.entityManager = entityManager;
		this.extractor = PersistenceProvider.fromEntityManager(entityManager);
		addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {

			if (hasMethodReturningStream(repositoryInformation.getRepositoryInterface())) {
				factory.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
			}
		});

		if (extractor.equals(PersistenceProvider.ECLIPSELINK)) {
			addQueryCreationListener(new EclipseLinkProjectionQueryCreationListener(entityManager));
		}
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
		return Optional.of(new DozerQueryLookupStrategy(dozerMapper, conversionServiceName, beanFactory, JpaQueryLookupStrategy.create(entityManager, key,
				extractor, evaluationContextProvider, EscapeCharacter.of(escapeCharacter.getEscapeCharacter()))));
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

	/**
	 * Query creation listener that informs EclipseLink users that they have to be
	 * extra careful when defining repository query methods using projections as we
	 * have to rely on the declaration order of the accessors in projection
	 * interfaces matching the order in columns. Alias-based mapping doesn't work
	 * with EclipseLink as it doesn't support {@link Tuple} based queries yet.
	 *
	 * @author Oliver Gierke
	 * @since 2.0.5
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=289141
	 */
	@Slf4j
	private static class EclipseLinkProjectionQueryCreationListener implements QueryCreationListener<AbstractJpaQuery> {

		private static final String ECLIPSELINK_PROJECTIONS = "Usage of Spring Data projections detected on persistence provider EclipseLink. Make sure the following query methods declare result columns in exactly the order the accessors are declared in the projecting interface or the order of parameters for DTOs:";

		private final JpaMetamodel metamodel;

		private boolean warningLogged = false;

		/**
		 * Creates a new {@link EclipseLinkProjectionQueryCreationListener} for the
		 * given {@link EntityManager}.
		 *
		 * @param em must not be {@literal null}.
		 */
		public EclipseLinkProjectionQueryCreationListener(EntityManager em) {

			Assert.notNull(em, "EntityManager must not be null!");

			this.metamodel = JpaMetamodel.of(em.getMetamodel());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.repository.core.support.QueryCreationListener#
		 * onCreation(org.springframework.data.repository.query.RepositoryQuery)
		 */
		@Override
		public void onCreation(AbstractJpaQuery query) {

			JpaQueryMethod queryMethod = query.getQueryMethod();
			ReturnedType type = queryMethod.getResultProcessor().getReturnedType();

			if (type.isProjecting() && !metamodel.isJpaManaged(type.getReturnedType())) {

				if (!warningLogged) {
					log.info(ECLIPSELINK_PROJECTIONS);
					this.warningLogged = true;
				}

				log.info(" - {}", queryMethod);
			}
		}
	}
}
