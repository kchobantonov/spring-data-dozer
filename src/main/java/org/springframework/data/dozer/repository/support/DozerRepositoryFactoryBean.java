package org.springframework.data.dozer.repository.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.dozer.repository.query.EscapeCharacter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.github.dozermapper.core.Mapper;

public class DozerRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends RepositoryFactoryBeanSupport<T, S, ID> implements ApplicationListener {

	private @Nullable Mapper dozerMapper;
	private EntityPathResolver entityPathResolver;
	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
	private BeanFactory beanFactory;
	private MappingContext<?, ?> mappingContext;
	
	private DozerRepositoryFactory dozerRepositoryFactory;

	/**
	 * Creates a new {@link DozerRepositoryFactoryBean} for the given repository
	 * interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public DozerRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
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
	protected RepositoryFactorySupport createRepositoryFactory() {
		dozerRepositoryFactory = new DozerRepositoryFactory(dozerMapper, beanFactory, mappingContext);
		dozerRepositoryFactory.setEntityPathResolver(entityPathResolver);
		dozerRepositoryFactory.setEscapeCharacter(escapeCharacter);
		return dozerRepositoryFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		this.beanFactory = beanFactory;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent && dozerRepositoryFactory != null) {
			dozerRepositoryFactory.validateAfterRefresh();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		Assert.state(dozerMapper != null, "Mapper must not be null!");

		super.afterPropertiesSet();
	}

	public void setDozerMapper(Mapper dozerMapper) {

		this.dozerMapper = dozerMapper;
	}

	public void setEscapeCharacter(char escapeCharacter) {

		this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
	}
}
