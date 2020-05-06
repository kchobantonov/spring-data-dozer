package org.springframework.data.dozer.repository.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.util.ProxyUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Similar class like
 * {@see org.springframework.data.repository.support.Repositories} but returns
 * all registered repositories for a specific domain class
 * 
 * @author kchobantonov
 */
public class Repositories implements Iterable<Class<?>>, ApplicationContextAware {

	static final Repositories NONE = new Repositories();

	private static final String DOMAIN_TYPE_MUST_NOT_BE_NULL = "Domain type must not be null!";

	private Optional<BeanFactory> beanFactory;
	private Map<Class<?>, Set<String>> repositoryBeanNames;
	private Map<Class<?>, Map<String, RepositoryFactoryInformation<Object, Object>>> repositoryFactoryInfos;

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {

		this.beanFactory = Optional.empty();
		this.repositoryBeanNames = Collections.emptyMap();
		this.repositoryFactoryInfos = Collections.emptyMap();
	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository
	 * instances and meta information from the given {@link ListableBeanFactory}.
	 *
	 * @param factory must not be {@literal null}.
	 */
	public Repositories(ListableBeanFactory factory) {

		Assert.notNull(factory, "ListableBeanFactory must not be null!");

		this.beanFactory = Optional.of(factory);
		this.repositoryFactoryInfos = new HashMap<>();
		this.repositoryBeanNames = new HashMap<>();

		populateRepositoryFactoryInformation(factory);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = Optional.of(applicationContext);
		this.repositoryFactoryInfos = new HashMap<>();
		this.repositoryBeanNames = new HashMap<>();

		populateRepositoryFactoryInformation(applicationContext);
	}

	private void populateRepositoryFactoryInformation(ListableBeanFactory factory) {

		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory,
				RepositoryFactoryInformation.class, false, false)) {
			cacheRepositoryFactory(name);
		}
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given domain class or
	 *         {@literal Optional#empty()} if no repository registered for this
	 *         domain class.
	 */
	public Optional<Map<String, RepositoryInformation>> getRepositoryInformationFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Map<String, RepositoryFactoryInformation<Object, Object>> information = getRepositoryFactoryInfoFor(
				domainClass);
		if (information == null) {
			return Optional.empty();
		}
		return Optional.of(information.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
				e -> (RepositoryInformation) e.getValue().getRepositoryInformation())));
	}

	@SuppressWarnings("rawtypes")
	private synchronized void cacheRepositoryFactory(String name) {

		RepositoryFactoryInformation repositoryFactoryInformation = beanFactory.get().getBean(name,
				RepositoryFactoryInformation.class);
		Class<?> domainType = ClassUtils
				.getUserClass(repositoryFactoryInformation.getRepositoryInformation().getDomainType());

		RepositoryInformation information = repositoryFactoryInformation.getRepositoryInformation();
		Set<Class<?>> alternativeDomainTypes = information.getAlternativeDomainTypes();

		Set<Class<?>> typesToRegister = new HashSet<>(alternativeDomainTypes.size() + 1);
		typesToRegister.add(domainType);
		typesToRegister.addAll(alternativeDomainTypes);

		for (Class<?> type : typesToRegister) {
			cache(type, repositoryFactoryInformation, BeanFactoryUtils.transformedBeanName(name));
		}
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances
	 * of the given domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Class<?> userClass = ProxyUtils.getUserClass(domainClass);

		return repositoryFactoryInfos.containsKey(userClass);
	}

	/**
	 * Returns the repositories managing the given domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public Optional<Map<String, Object>> getRepositoriesFor(Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Class<?> userClass = ProxyUtils.getUserClass(domainClass);
		Optional<Set<String>> repositoryBeanName = Optional.ofNullable(repositoryBeanNames.get(userClass));

		return beanFactory.flatMap(it -> repositoryBeanName
				.map(beans -> beans.stream().collect(Collectors.toMap(Function.identity(), it::getBean))));
	}

	/**
	 * Returns the {@link RepositoryFactoryInformation} for the given domain class.
	 * The given <code>code</code> is converted to the actual user class if
	 * necessary, @see ProxyUtils#getUserClass.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryFactoryInformation} for the given domain class
	 *         or {@literal null} if no repository registered for this domain class.
	 */
	private Map<String, RepositoryFactoryInformation<Object, Object>> getRepositoryFactoryInfoFor(
			Class<?> domainClass) {

		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

		Class<?> userType = ProxyUtils.getUserClass(domainClass);
		Map<String, RepositoryFactoryInformation<Object, Object>> repositoryInfo = repositoryFactoryInfos.get(userType);

		if (repositoryInfo != null) {
			return repositoryInfo;
		}

		if (!userType.equals(Object.class)) {
			return getRepositoryFactoryInfoFor(userType.getSuperclass());
		}

		return Collections.emptyMap();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Class<?>> iterator() {
		return repositoryFactoryInfos.keySet().iterator();
	}

	/**
	 * Caches the repository information for the given domain type or overrides
	 * existing information in case the bean name points to a primary bean
	 * definition.
	 * 
	 * @param type        must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @param name        must not be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void cache(Class<?> type, RepositoryFactoryInformation information, String name) {

		Map<String, RepositoryFactoryInformation<Object, Object>> value = this.repositoryFactoryInfos.get(type);
		if (value == null) {
			value = new HashMap<>();
		}

		if (!value.containsKey(name)) {
			value.put(name, information);
		}

		Set<String> names = this.repositoryBeanNames.get(type);
		if (names == null) {
			names = new HashSet<>();
		}
		if (!names.contains(name)) {
			names.add(name);
		}

		this.repositoryFactoryInfos.put(type, value);
		this.repositoryBeanNames.put(type, names);
	}

}