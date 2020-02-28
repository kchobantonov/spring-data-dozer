package org.springframework.data.dozer.repository.support;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
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

	protected final DozerEntityInformation<T, ?> entityInformation;
	protected PersistentEntity<?, ?> adaptedPersistentEntity;
	protected final Mapper dozerMapper;
	protected final ListableBeanFactory beanFactory;

	protected final Lazy<Optional<RepositoryInformation>> adaptedRepositoryInformation;
	protected final Lazy<Optional<PagingAndSortingRepository<Object, Object>>> adaptedRepository;
	protected final Lazy<ConversionService> conversionService;

	protected boolean useConverterServiceForEntityToAdaptedEntity = false;
	protected boolean useConverterServiceForAdaptedEntityToEntity = false;
	protected boolean useConverterServiceForEntityIdToAdaptedEntityId = false;
	protected boolean useConverterServiceForAdaptedEntityIdToEntityId = false;
	protected Method entityIdSetter;

	public SimpleDozerRepository(DozerEntityInformation<T, ?> entityInformation, Mapper dozerMapper,
			String conversionServiceName, BeanFactory beanFactory) {

		Assert.notNull(entityInformation, "DozerEntityInformation must not be null!");
		Assert.notNull(dozerMapper, "Mapper must not be null!");
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory, "beanFactory must be of type ListableBeanFactory!");
		this.beanFactory = (ListableBeanFactory) beanFactory;

		this.entityInformation = entityInformation;
		this.dozerMapper = dozerMapper;
		this.conversionService = Lazy
				.of(() -> this.beanFactory.getBeansOfType(ConversionService.class).get(conversionServiceName));

		this.adaptedRepositoryInformation = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoryInformationFor(entityInformation.getAdaptedJavaType()));

		this.adaptedRepository = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoryFor(entityInformation.getAdaptedJavaType())
				.map(r -> (PagingAndSortingRepository<Object, Object>) r));
	}

	protected RepositoryInformation getAdaptedRepositoryInformation() {
		return adaptedRepositoryInformation.get().get();
	}

	protected PagingAndSortingRepository<Object, Object> getAdaptedRepository() {
		return adaptedRepository.get().get();
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

		// validate domain model mappings
		try {
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getJavaType(),
					entityInformation.getAdaptedJavaType());
		} catch (MetadataLookupException e) {
			if (!considerConversionServiceForEntityMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getJavaType(), entityInformation.getAdaptedJavaType())) {
				throw e;
			}
			useConverterServiceForEntityToAdaptedEntity = true;
		}

		try {
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getAdaptedJavaType(),
					entityInformation.getJavaType());
		} catch (MetadataLookupException e) {
			if (!considerConversionServiceForEntityMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getAdaptedJavaType(), entityInformation.getJavaType())) {
				throw e;
			}
			useConverterServiceForAdaptedEntityToEntity = true;
		}

		// validate domain model id fields mappings
		try {
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getIdType(),
					getAdaptedRepositoryInformation().getIdType());
		} catch (MetadataLookupException e) {
			if (!considerConversionServiceForEntityIdMapping || !conversionService.getOptional().get()
					.canConvert(entityInformation.getIdType(), getAdaptedRepositoryInformation().getIdType())) {
				throw e;
			}
			useConverterServiceForEntityIdToAdaptedEntityId = true;
		}

		try {
			dozerMapper.getMappingMetadata().getClassMapping(getAdaptedRepositoryInformation().getIdType(),
					entityInformation.getIdType());
		} catch (MetadataLookupException e) {
			if (!considerConversionServiceForEntityIdMapping || !conversionService.getOptional().get()
					.canConvert(getAdaptedRepositoryInformation().getIdType(), entityInformation.getIdType())) {
				throw e;
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
	protected <S extends T> S toAdaptedEntity(S resource, Object entity) {
		if (StringUtils.isEmpty(entityInformation.getDozerMapId())) {
			dozerMapper.map(resource, entity);
		} else {
			dozerMapper.map(resource, entity, entityInformation.getDozerMapId());
		}
		return resource;
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
		Iterable<?> entities = getAdaptedRepository().findAll(sort);

		Iterable<T> resources = Iterables.transform(entities, source -> toDozerEntity(source));

		return resources;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Page<?> entities = getAdaptedRepository().findAll(pageable);

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
		if (resourceId != null) {
			Object entityId;
			try {
				entityId = toAdaptedId(resourceId);
			} catch (MappingException e) {
				return;
			}

			if (entityId != null) {
				getAdaptedRepository().deleteById(entityId);
			}
		}
	}

	@Override
	public void delete(T resource) {
		deleteById((ID) entityInformation.getId(resource));
	}

	@Override
	public void deleteAll(Iterable<? extends T> resources) {
		Iterable<Object> entityIds = Iterables.transform(resources, source -> dozerMapper
				.map(entityInformation.getId(source), getAdaptedRepositoryInformation().getIdType()));

		for (Object entityId : entityIds) {
			if (entityId != null) {
				getAdaptedRepository().deleteById(entityId);
			}
		}
	}

	@Override
	public void deleteAll() {
		getAdaptedRepository().deleteAll();
	}

}
