package org.springframework.data.dozer.repository.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.data.dozer.repository.DozerRepository;
import org.springframework.data.dozer.repository.support.DozerEvaluationContextExtension;
import org.springframework.data.dozer.repository.support.DozerRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

public class DozerRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {
	private static final String MAPPING_CONTEXT_BEAN_NAME = "dozerMappingContext";
	private static final String DEFAULT_DOZER_MAPPER_BEAN_NAME = "dozerMapper";
	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return "DOZER";
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
		return DozerRepositoryFactoryBean.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config14.
	 * RepositoryConfigurationExtensionSupport#getModulePrefix()
	 */
	@Override
	protected String getModulePrefix() {
		return getModuleName().toLowerCase(Locale.US);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#getIdentifyingAnnotations()
	 */
	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(DozerEntity.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#getIdentifyingTypes()
	 */
	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.<Class<?>>singleton(DozerRepository.class);
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

		Optional<String> dozerMapperRef = source.getAttribute("dozerMapperRef");
		builder.addPropertyReference("dozerMapper", dozerMapperRef.orElse(DEFAULT_DOZER_MAPPER_BEAN_NAME));
		builder.addPropertyValue(ESCAPE_CHARACTER_PROPERTY, getEscapeCharacter(source).orElse('\\'));
		builder.addPropertyReference("mappingContext", MAPPING_CONTEXT_BEAN_NAME);
	}

	/**
	 * XML configurations do not support {@link Character} values. This method
	 * catches the exception thrown and returns an {@link Optional#empty()} instead.
	 */
	private static Optional<Character> getEscapeCharacter(RepositoryConfigurationSource source) {

		try {
			return source.getAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class);
		} catch (IllegalArgumentException ___) {
			return Optional.empty();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.config.
	 * RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.
	 * springframework.beans.factory.support.BeanDefinitionRegistry,
	 * org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();

		registerLazyIfNotAlreadyRegistered(() -> new RootBeanDefinition(DozerMetamodelMappingContextFactoryBean.class),
				registry, MAPPING_CONTEXT_BEAN_NAME, source);

		// EvaluationContextExtension for DOZER specific SpEL functions

		registerIfNotAlreadyRegistered(() -> {

			Object value = AnnotationRepositoryConfigurationSource.class.isInstance(config) //
					? config.getRequiredAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class) //
					: config.getAttribute(ESCAPE_CHARACTER_PROPERTY).orElse("\\");

			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.rootBeanDefinition(DozerEvaluationContextExtension.class);
			builder.addConstructorArgValue(value);

			return builder.getBeanDefinition();

		}, registry, DozerEvaluationContextExtension.class.getName(), source);
	}

}
