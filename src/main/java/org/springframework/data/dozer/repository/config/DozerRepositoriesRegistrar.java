package org.springframework.data.dozer.repository.config;

import java.lang.annotation.Annotation;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

class DozerRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport#getAnnotation()
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableDozerRepositories.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport#getExtension()
	 */
	@Override
	protected RepositoryConfigurationExtension getExtension() {
		return new DozerRepositoryConfigExtension();
	}
}
