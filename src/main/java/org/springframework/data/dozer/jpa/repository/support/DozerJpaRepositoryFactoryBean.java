package org.springframework.data.dozer.jpa.repository.support;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.dozer.repository.query.EscapeCharacter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.github.dozermapper.core.Mapper;

public class DozerJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID>
		implements BeanFactoryAware, ApplicationListener<ContextRefreshedEvent> {

	private @Nullable EntityManager entityManager;
	protected @Nullable Mapper dozerMapper;
	protected String conversionServiceName;
	protected EntityPathResolver entityPathResolver;
	protected EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
	protected BeanFactory beanFactory;
	protected MappingContext<?, ?> mappingContext;

	private DozerJpaRepositoryFactory dozerRepositoryFactory;

	/**
	 * Creates a new {@link DozerJpaRepositoryFactoryBean} for the given repository
	 * interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public DozerJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}
	
	/**
	 * The {@link EntityManager} to be used.
	 *
	 * @param entityManager the entityManager to set
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
	 * #setMappingContext(org.springframework.data.mapping.context.MappingContext)
	 */
	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
		this.mappingContext = mappingContext;
	}

	/**
	 * Configures the {@link EntityPathResolver} to be used. Will expect a canonical
	 * bean to be present but fallback to {@link SimpleEntityPathResolver#INSTANCE}
	 * in case none is available.
	 *
	 * @param resolver must not be {@literal null}.
	 */
	@Autowired
	public void setEntityPathResolver(ObjectProvider<EntityPathResolver> resolver) {
		this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		this.beanFactory = beanFactory;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (dozerRepositoryFactory != null) {
			dozerRepositoryFactory.validateAfterRefresh();
		}
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		DozerJpaRepositoryFactory dozerJpaRepositoryFactory = new DozerJpaRepositoryFactory(entityManager, dozerMapper,
				conversionServiceName, beanFactory, mappingContext);
		dozerJpaRepositoryFactory.setEntityPathResolver(entityPathResolver);
		dozerJpaRepositoryFactory.setEscapeCharacter(escapeCharacter);

		return dozerJpaRepositoryFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		Assert.state(entityManager != null, "EntityManager must not be null!");
		Assert.state(dozerMapper != null, "Mapper must not be null!");

		super.afterPropertiesSet();
	}

	public void setDozerMapper(Mapper dozerMapper) {
		this.dozerMapper = dozerMapper;
	}

	public void setConversionServiceName(String conversionServiceName) {
		this.conversionServiceName = conversionServiceName;
	}

	public void setEscapeCharacter(char escapeCharacter) {

		this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
	}
}
