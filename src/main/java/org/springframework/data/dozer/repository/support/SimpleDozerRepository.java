package org.springframework.data.dozer.repository.support;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.dozer.annotation.DozerEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.MappingException;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class SimpleDozerRepository<T, ID> implements DozerRepositoryImplementation<T, ID>, BeanPostProcessor {

	private final DozerEntityInformation<T, ?> entityInformation;
	private final Mapper dozerMapper;

	private final Lazy<Optional<RepositoryInformation>> adaptedRepositoryInformation;
	private final Lazy<Optional<PagingAndSortingRepository<Object, Object>>> adaptedRepository;

	public SimpleDozerRepository(DozerEntityInformation<T, ?> entityInformation, Mapper dozerMapper,
			BeanFactory beanFactory) {

		Assert.notNull(entityInformation, "DozerEntityInformation must not be null!");
		Assert.notNull(dozerMapper, "Mapper must not be null!");
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory, "beanFactory must be of type ListableBeanFactory!");

		this.entityInformation = entityInformation;
		this.dozerMapper = dozerMapper;

		this.adaptedRepositoryInformation = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoryInformationFor(entityInformation.getAdaptedJavaType()));

		this.adaptedRepository = Lazy.of(() -> beanFactory.getBeanProvider(Repositories.class)
				.getIfAvailable(() -> new Repositories((ListableBeanFactory) beanFactory))
				.getRepositoryFor(entityInformation.getAdaptedJavaType())
				.map(r -> (PagingAndSortingRepository<Object, Object>) r));
	}

	private RepositoryInformation getAdaptedRepositoryInformation() {
		return adaptedRepositoryInformation.get().get();
	}

	private PagingAndSortingRepository<Object, Object> getAdaptedRepository() {
		return adaptedRepository.get().get();
	}

	@Override
	public void validateAfterRefresh() {
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

		if (!entityInformation.getAdaptedJavaType().isAssignableFrom(entityInformation.getJavaType())) {
			// validate domain model mappings
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getJavaType(),
					entityInformation.getAdaptedJavaType());
		}
		if (!entityInformation.getJavaType().isAssignableFrom(entityInformation.getAdaptedJavaType())) {
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getAdaptedJavaType(),
					entityInformation.getJavaType());
		}

		// validate domain model id fields mappings
		if (!getAdaptedRepositoryInformation().getIdType().isAssignableFrom(entityInformation.getIdType())) {
			dozerMapper.getMappingMetadata().getClassMapping(entityInformation.getIdType(),
					getAdaptedRepositoryInformation().getIdType());
		}
		if (!entityInformation.getIdType().isAssignableFrom(getAdaptedRepositoryInformation().getIdType())) {
			dozerMapper.getMappingMetadata().getClassMapping(getAdaptedRepositoryInformation().getIdType(),
					entityInformation.getIdType());
		}
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		Iterable<?> entities = getAdaptedRepository().findAll(sort);

		Iterable<T> resources = Iterables.transform(entities,
				source -> dozerMapper.map(source, entityInformation.getJavaType()));

		return resources;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Page<?> entities = getAdaptedRepository().findAll(pageable);

		Page<T> resources = entities.map(source -> dozerMapper.map(source, entityInformation.getJavaType()));

		return resources;
	}

	@Override
	public <S extends T> S save(S resource) {
		Object entity = dozerMapper.map(resource, entityInformation.getAdaptedJavaType());

		getAdaptedRepository().save(entity);

		return resource;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> resources) {
		Iterable<?> entities = Iterables.transform(resources,
				source -> dozerMapper.map(source, entityInformation.getAdaptedJavaType()));

		getAdaptedRepository().saveAll(entities);

		return resources;
	}

	@Override
	public Optional<T> findById(ID resourceId) {
		Object entityId;
		try {
			entityId = dozerMapper.map(resourceId, getAdaptedRepositoryInformation().getIdType());
		} catch (MappingException e) {
			return Optional.empty();
		}

		Optional<Object> entity = getAdaptedRepository().findById(entityId);

		return entity.map(source -> dozerMapper.map(source, entityInformation.getJavaType()));
	}

	@Override
	public boolean existsById(ID resourceId) {
		Object entityId;
		try {
			entityId = dozerMapper.map(resourceId, getAdaptedRepositoryInformation().getIdType());
		} catch (MappingException e) {
			return false;
		}
		return getAdaptedRepository().existsById(entityId);
	}

	@Override
	public Iterable<T> findAll() {
		Iterable<?> entities = getAdaptedRepository().findAll();

		Iterable<T> resources = Iterables.transform(entities,
				source -> dozerMapper.map(source, entityInformation.getJavaType()));

		return resources;
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> resourceIds) {
		Iterable<Object> entityIds = Iterables.filter(Iterables.transform(resourceIds, source -> {
			try {
				return dozerMapper.map(source, getAdaptedRepositoryInformation().getIdType());
			} catch (MappingException e) {
				return null;
			}
		}), Predicates.notNull());

		Iterable<Object> entities = getAdaptedRepository().findAllById(entityIds);

		Iterable<T> resources = Iterables.transform(entities,
				source -> dozerMapper.map(source, entityInformation.getJavaType()));

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
				entityId = dozerMapper.map(resourceId, getAdaptedRepositoryInformation().getIdType());
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
