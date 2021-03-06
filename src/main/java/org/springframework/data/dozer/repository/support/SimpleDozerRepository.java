package org.springframework.data.dozer.repository.support;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.data.dozer.annotation.DozerRepository;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.MappingException;
import com.github.dozermapper.core.metadata.MetadataLookupException;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class SimpleDozerRepository<T, ID> implements DozerRepositoryImplementation<T, ID>, BeanPostProcessor {
	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	protected final RepositoryInformation repositoryInformation;
	protected final DozerEntityInformation<T, ?> entityInformation;
	protected PersistentEntity<?, ?> adaptedPersistentEntity;
	protected final Mapper dozerMapper;
	protected final ListableBeanFactory beanFactory;

	protected final Lazy<Optional<Map<String, RepositoryInformation>>> adaptedRepositoryInformations;
	protected final Lazy<Optional<Map<String, Object>>> adaptedRepositories;

	private PagingAndSortingRepository<Object, Object> adaptedRepository;
	private String adaptedRepositoryName;
	private RepositoryInformation adaptedRepositoryInformation;

	protected final Lazy<ConversionService> conversionService;

	protected boolean useConverterServiceForEntityToAdaptedEntity = false;
	protected boolean useConverterServiceForAdaptedEntityToEntity = false;
	protected boolean useConverterServiceForEntityIdToAdaptedEntityId = false;
	protected boolean useConverterServiceForAdaptedEntityIdToEntityId = false;
	protected Method entityIdSetter;

	protected Map<String, String> dozerEntityFieldNameToAdaptedFieldName;

	public SimpleDozerRepository(RepositoryInformation repositoryInformation,
			DozerEntityInformation<T, ?> entityInformation, Mapper dozerMapper, String conversionServiceName,
			BeanFactory beanFactory) {

		Assert.notNull(repositoryInformation, "RepositoryInformation must not be null!");
		Assert.notNull(entityInformation, "DozerEntityInformation must not be null!");
		Assert.notNull(dozerMapper, "Mapper must not be null!");
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory, "beanFactory must be of type ListableBeanFactory!");
		this.beanFactory = (ListableBeanFactory) beanFactory;

		this.repositoryInformation = repositoryInformation;
		this.entityInformation = entityInformation;
		this.dozerMapper = dozerMapper;
		this.conversionService = Lazy
				.of(() -> this.beanFactory.getBeansOfType(ConversionService.class).get(conversionServiceName));

		this.adaptedRepositoryInformations = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoryInformationFor(entityInformation.getAdaptedJavaType()));

		this.adaptedRepositories = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoriesFor(entityInformation.getAdaptedJavaType()));
	}

	protected RepositoryInformation getAdaptedRepositoryInformation() {
		if (adaptedRepositoryInformation != null) {
			return adaptedRepositoryInformation;
		}

		getAdaptedRepository();

		Map<String, RepositoryInformation> informations = adaptedRepositoryInformations.get().get();
		if (informations.size() > 1) {
			adaptedRepositoryInformation = Optional.ofNullable(informations.get(adaptedRepositoryName)).get();
		} else {
			adaptedRepositoryInformation = informations.values().iterator().next();
		}

		return adaptedRepositoryInformation;
	}

	protected PagingAndSortingRepository<Object, Object> getAdaptedRepository() {
		if (adaptedRepository != null) {
			return adaptedRepository;
		}
		Map<String, Object> repositories = adaptedRepositories.get().get();
		Object repo = null;
		if (repositories.size() > 1) {
			DozerRepository adaptedDozerRepository = AnnotatedElementUtils
					.findMergedAnnotation(repositoryInformation.getRepositoryInterface(), DozerRepository.class);

			Assert.notNull(adaptedDozerRepository,
					"Multiple adapted repositories found for " + entityInformation.getAdaptedJavaType()
							+ " to support dozer entity " + entityInformation.getJavaType()
							+ ". Please specify a concrete adapted repository interface using annnotation "
							+ DozerRepository.class + " attribute adaptedRepositoryClass on "
							+ repositoryInformation.getRepositoryInterface());

			List<String> matchesRepositoryNames = repositories.entrySet().stream().filter(
					it -> adaptedDozerRepository.adaptedRepositoryClass().isAssignableFrom(it.getValue().getClass()))
					.map(it -> it.getKey()).collect(Collectors.toList());

			Assert.isTrue(matchesRepositoryNames.size() > 0,
					"Unable to find repository information for " + entityInformation.getAdaptedJavaType()
							+ " and adapter repository class " + adaptedDozerRepository.adaptedRepositoryClass()
							+ " to support dozer entity " + entityInformation.getJavaType() + ". Validate annnotation "
							+ DozerRepository.class + " attribute adaptedRepositoryClass");

			Assert.isTrue(matchesRepositoryNames.size() == 1,
					"Unable to find unique repository information for " + entityInformation.getAdaptedJavaType()
							+ " and adapter repository class " + adaptedDozerRepository.adaptedRepositoryClass()
							+ " to support dozer entity " + entityInformation.getJavaType()
							+ ". Narrow the repository interface by applying a change in annnotation "
							+ DozerRepository.class + " attribute adaptedRepositoryClass. Repositories found :"
							+ matchesRepositoryNames);

			adaptedRepositoryName = matchesRepositoryNames.get(0);
			repo = repositories.get(adaptedRepositoryName);
		} else {
			repo = repositories.values().iterator().next();
		}

		Assert.isInstanceOf(PagingAndSortingRepository.class, repo,
				"Unsupported adapted repository for " + entityInformation.getAdaptedJavaType()
						+ " to support dozer entity " + entityInformation.getJavaType()
						+ ". Adapted repository have to implement " + PagingAndSortingRepository.class);
		adaptedRepository = (PagingAndSortingRepository) repo;

		return adaptedRepository;
	}

	@Override
	public void validateAfterRefresh(PersistentEntities persistentEntities) {
		try {
			getAdaptedRepositoryInformation();
		} catch (NoSuchElementException e) {
			throw new IllegalStateException(
					"Unable to find repository information for " + entityInformation.getAdaptedJavaType()
							+ " to support dozer entity " + entityInformation.getJavaType() + ". Validate annotation "
							+ DozerEntity.class + " attribute domainClass",
					e);
		}
		try {
			getAdaptedRepository();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to find " + PagingAndSortingRepository.class + " repository for "
					+ entityInformation.getAdaptedJavaType() + " to support dozer entity "
					+ entityInformation.getJavaType() + ". Validate annotation " + DozerEntity.class
					+ " attribute domainClass", e);
		}

		adaptedPersistentEntity = persistentEntities
				.getRequiredPersistentEntity(entityInformation.getAdaptedJavaType());

		boolean considerConversionServiceForEntityMapping = entityInformation.getMapEntityUsingConvertionService()
				&& conversionService.getOptional().isPresent();
		boolean considerConversionServiceForEntityIdMapping = entityInformation.getMapEntityIdUsingConvertionService()
				&& conversionService.getOptional().isPresent();

		DozerUtil dozerUtil = DozerUtilFactory.getInstance().getDozerUtil(dozerMapper);

		dozerEntityFieldNameToAdaptedFieldName = dozerUtil
				.getDozerEntityFieldNameToAdaptedFieldNameMap(entityInformation);

		// validate domain model mappings
		if (!dozerUtil.hasDozerMapping(entityInformation.getJavaType(), entityInformation.getAdaptedJavaType(),
				entityInformation.getDozerMapId())) {

			if (!considerConversionServiceForEntityMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getJavaType(), entityInformation.getAdaptedJavaType())) {
				throw new MetadataLookupException(
						"No mapping definition found for: " + entityInformation.getJavaType().getName() + " -> "
								+ entityInformation.getAdaptedJavaType().getName() + ".");
			}
			useConverterServiceForEntityToAdaptedEntity = true;
		}

		if (!dozerUtil.hasDozerMapping(entityInformation.getAdaptedJavaType(), entityInformation.getJavaType(),
				entityInformation.getDozerMapId())) {

			if (!considerConversionServiceForEntityMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getAdaptedJavaType(), entityInformation.getJavaType())) {
				throw new MetadataLookupException(
						"No mapping definition found for: " + entityInformation.getAdaptedJavaType().getName() + " -> "
								+ entityInformation.getJavaType().getName() + ".");
			}
			useConverterServiceForAdaptedEntityToEntity = true;
		}

		// validate domain model id fields mappings
		if (!dozerUtil.hasDozerMapping(entityInformation.getIdType(), getAdaptedRepositoryInformation().getIdType(),
				null)) {
			if (!considerConversionServiceForEntityIdMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getIdType(), getAdaptedRepositoryInformation().getIdType())) {
				throw new MetadataLookupException(
						"No mapping definition found for: " + entityInformation.getIdType().getName() + " -> "
								+ getAdaptedRepositoryInformation().getIdType().getName() + ".");
			}
			useConverterServiceForEntityIdToAdaptedEntityId = true;
		}

		if (!dozerUtil.hasDozerMapping(getAdaptedRepositoryInformation().getIdType(), entityInformation.getIdType(),
				null)) {
			if (!considerConversionServiceForEntityIdMapping || !conversionService.getOptional().get()
					.canConvert(getAdaptedRepositoryInformation().getIdType(), entityInformation.getIdType())) {
				throw new MetadataLookupException(
						"No mapping definition found for: " + getAdaptedRepositoryInformation().getIdType().getName()
								+ " -> " + entityInformation.getIdType().getName() + ".");
			}
			useConverterServiceForAdaptedEntityIdToEntityId = true;
		}

		entityIdSetter = entityInformation.getPersistentEntity().getRequiredIdProperty().getRequiredSetter();
	}

	protected T toDozerEntity(Object source) {
		if (useConverterServiceForAdaptedEntityToEntity) {
			return conversionService.getOptional().get().convert(source, entityInformation.getJavaType());
		}

		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			return dozerMapper.map(source, entityInformation.getJavaType());
		}
		return dozerMapper.map(source, entityInformation.getJavaType(), entityInformation.getDozerMapId());
	}

	/**
	 * Invoked when we need to map the resource to newly created adapted entity
	 * 
	 * @param <S>
	 * @param resource
	 * @return
	 */
	protected <S extends T> Object toAdaptedEntity(S resource) {
		if (useConverterServiceForEntityToAdaptedEntity) {
			return conversionService.getOptional().get().convert(resource, entityInformation.getAdaptedJavaType());
		}

		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			return dozerMapper.map(resource, entityInformation.getAdaptedJavaType());
		}
		return dozerMapper.map(resource, entityInformation.getAdaptedJavaType(), entityInformation.getDozerMapId());
	}

	/**
	 * Invoked when we need to map the resource data to an existing adapted entity
	 * 
	 * @param <S>
	 * @param resource
	 * @param entity
	 * @return
	 */
	protected <S extends T> Object toAdaptedEntity(S resource, Object entity) {
		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			dozerMapper.map(resource, entity);
		} else {
			dozerMapper.map(resource, entity, entityInformation.getDozerMapId());
		}
		return entity;
	}

	protected Object toAdaptedId(ID resourceId) {
		if (useConverterServiceForEntityIdToAdaptedEntityId) {
			return conversionService.getOptional().get().convert(resourceId,
					getAdaptedRepositoryInformation().getIdType());
		}

		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			return dozerMapper.map(resourceId, getAdaptedRepositoryInformation().getIdType());
		}
		return dozerMapper.map(resourceId, getAdaptedRepositoryInformation().getIdType(),
				entityInformation.getDozerMapId());
	}

	protected Object toResourceId(Object sourceId) {
		if (useConverterServiceForAdaptedEntityIdToEntityId) {
			return conversionService.getOptional().get().convert(sourceId, entityInformation.getIdType());
		}

		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			return dozerMapper.map(sourceId, entityInformation.getIdType());
		}
		return dozerMapper.map(sourceId, entityInformation.getIdType(), entityInformation.getDozerMapId());
	}

	protected <S extends T> S toResource(S resource, Object entity) {
		// apply id
		Object entityId = adaptedPersistentEntity.getIdentifierAccessor(entity).getRequiredIdentifier();
		try {
			entityIdSetter.invoke(resource, toResourceId(entityId));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new MappingException(e);
		}

		// apply version
		if (entityInformation.getPersistentEntity().hasVersionProperty()
				&& adaptedPersistentEntity.hasVersionProperty()) {

			Field adaptedVersionField = adaptedPersistentEntity.getRequiredVersionProperty().getRequiredField();
			ReflectionUtils.makeAccessible(adaptedVersionField);
			Object entityVersion = ReflectionUtils.getField(adaptedVersionField, entity);

			Field versionField = entityInformation.getPersistentEntity().getRequiredVersionProperty()
					.getRequiredField();
			ReflectionUtils.makeAccessible(versionField);
			ReflectionUtils.setField(versionField, resource,
					conversionService.getOptional().get().convert(entityVersion, versionField.getType()));
		}

		return resource;
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		Iterable<?> entities = getAdaptedRepository().findAll(toAdaptedSort(sort));

		Iterable<T> resources = Iterables.transform(entities, source -> toDozerEntity(source));

		return resources;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Page<?> entities = getAdaptedRepository().findAll(toAdaptedPageable(pageable));

		Page<T> resources = entities.map(source -> toDozerEntity(source));

		return resources;
	}

	@Override
	public <S extends T> S save(S resource) {

		Object entity = null;

		if (entityInformation.isNew(resource)) {
			entity = toAdaptedEntity(resource);
		} else {
			// do merge
			Object entityId;
			try {
				entityId = toAdaptedId((ID) entityInformation.getRequiredId(resource));
			} catch (MappingException e) {
				throw new IllegalArgumentException(e);
			}

			Optional<?> persistedEntity = getAdaptedRepository().findById(entityId);
			if (persistedEntity.isPresent()) {
				entity = persistedEntity.get();
				entity = toAdaptedEntity(resource, entity);
			} else {
				entity = toAdaptedEntity(resource);
			}

		}

		entity = getAdaptedRepository().save(entity);

		return toResource(resource, entity);
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> resources) {
		Assert.notNull(resources, "Entities must not be null!");

		List<S> result = new ArrayList<S>();

		for (S entity : resources) {
			result.add(save(entity));
		}

		return result;
	}

	@Override
	public Optional<T> findById(ID resourceId) {
		Object entityId;
		try {
			entityId = toAdaptedId(resourceId);
		} catch (MappingException e) {
			throw new IllegalArgumentException(e);
		}

		Optional<Object> entity = getAdaptedRepository().findById(entityId);

		return entity.map(source -> toDozerEntity(source));
	}

	@Override
	public boolean existsById(ID resourceId) {
		Object entityId;
		try {
			entityId = toAdaptedId(resourceId);
		} catch (MappingException e) {
			throw new IllegalArgumentException(e);
		}
		return getAdaptedRepository().existsById(entityId);
	}

	@Override
	public Iterable<T> findAll() {
		Iterable<?> entities = getAdaptedRepository().findAll();

		Iterable<T> resources = Iterables.transform(entities, source -> toDozerEntity(source));

		return resources;
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> resourceIds) {
		Iterable<Object> entityIds = Iterables.filter(Iterables.transform(resourceIds, source -> {
			try {
				return toAdaptedId(source);
			} catch (MappingException e) {
				return null;
			}
		}), Predicates.notNull());

		Iterable<Object> entities = getAdaptedRepository().findAllById(entityIds);

		Iterable<T> resources = Iterables.transform(entities, source -> toDozerEntity(source));

		return resources;
	}

	@Override
	public long count() {
		return getAdaptedRepository().count();
	}

	@Override
	public void deleteById(ID resourceId) {
		Assert.notNull(resourceId, ID_MUST_NOT_BE_NULL);

		delete(findById(resourceId).orElseThrow(() -> new EmptyResultDataAccessException(
				String.format("No %s entity with id %s exists!", entityInformation.getJavaType(), resourceId), 1)));
	}

	@Override
	public void delete(T resource) {
		if (entityInformation.isNew(resource)) {
			return;
		}
		
		Object entityId = toAdaptedId((ID) entityInformation.getId(resource));

		getAdaptedRepository().deleteById(entityId);
	}

	@Override
	public void deleteAll(Iterable<? extends T> resources) {
		Assert.notNull(resources, "Entities must not be null!");

		for (T entity : resources) {
			delete(entity);
		}
	}

	@Override
	public void deleteAll() {
		for (T element : findAll()) {
			delete(element);
		}
	}

	protected Sort toAdaptedSort(Sort sort) {
		if (sort.isSorted() && dozerEntityFieldNameToAdaptedFieldName != null
				&& !dozerEntityFieldNameToAdaptedFieldName.isEmpty()) {
			sort = Sort.by(sort.toList().stream().map(it -> toAdaptedOrder(it)).collect(Collectors.toList()));
		}

		return sort;
	}

	protected Order toAdaptedOrder(Order order) {
		if (dozerEntityFieldNameToAdaptedFieldName != null && !dozerEntityFieldNameToAdaptedFieldName.isEmpty()) {
			return order.withProperty(
					dozerEntityFieldNameToAdaptedFieldName.getOrDefault(order.getProperty(), order.getProperty()));
		}

		return order;
	}

	protected Pageable toAdaptedPageable(Pageable pageable) {
		if (pageable.getSort().isSorted() && dozerEntityFieldNameToAdaptedFieldName != null
				&& !dozerEntityFieldNameToAdaptedFieldName.isEmpty()) {
			pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
					toAdaptedSort(pageable.getSort()));
		}

		return pageable;
	}

}
