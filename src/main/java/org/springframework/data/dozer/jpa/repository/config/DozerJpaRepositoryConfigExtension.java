package org.springframework.data.dozer.jpa.repository.config;

import java.util.Optional;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.dozer.jpa.repository.support.DozerJpaRepositoryFactoryBean;
import org.springframework.data.dozer.repository.config.DozerRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.lang.Nullable;

public class DozerJpaRepositoryConfigExtension extends DozerRepositoryConfigExtension {
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return "DOZERJPA";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.config.RepositoryConfigurationExtension#
	 * getRepositoryFactoryBeanClassName()
	 */
	@Override
	public String getRepositoryFactoryBeanClassName() {
		return DozerJpaRepositoryFactoryBean.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans
	 * .factory.support.BeanDefinitionBuilder,
	 * org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
		super.postProcess(builder, source);

		Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		builder.addPropertyValue("entityManager", getEntityManagerBeanDefinitionFor(source, source.getSource()));

	}

	/**
	 * Creates an anonymous factory to extract the actual
	 * {@link javax.persistence.EntityManager} from the
	 * {@link javax.persistence.EntityManagerFactory} bean name reference.
	 *
	 * @param config
	 * @param source
	 * @return
	 */
	private static AbstractBeanDefinition getEntityManagerBeanDefinitionFor(RepositoryConfigurationSource config,
			@Nullable Object source) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition("org.springframework.orm.jpa.SharedEntityManagerCreator");
		builder.setFactoryMethod("createSharedEntityManager");
		builder.addConstructorArgReference(getEntityManagerBeanRef(config));

		AbstractBeanDefinition bean = builder.getRawBeanDefinition();
		bean.setSource(source);

		return bean;
	}

	private static String getEntityManagerBeanRef(RepositoryConfigurationSource config) {

		Optional<String> entityManagerFactoryRef = config.getAttribute("entityManagerFactoryRef");
		return entityManagerFactoryRef.orElse("entityManagerFactory");
	}

}
