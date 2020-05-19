package org.springframework.data.dozer.repository.support;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.dozer.repository.query.DozerEntityMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.cache.CacheKeyFactory;
import com.github.dozermapper.core.classmap.ClassMap;
import com.github.dozermapper.core.classmap.ClassMappings;
import com.github.dozermapper.core.classmap.Configuration;
import com.github.dozermapper.core.classmap.MappingDirection;
import com.github.dozermapper.core.config.BeanContainer;
import com.github.dozermapper.core.fieldmap.FieldMap;
import com.github.dozermapper.core.util.MappingUtils;

public class DozerUtil {
	private final Mapper dozerMapper;

	public DozerUtil(Mapper dozerMapper) {
		Assert.notNull(dozerMapper, "Mapper must not be null!");

		this.dozerMapper = dozerMapper;
	}

	public Map<String, String> getDozerEntityFieldNameToAdaptedFieldNameMap(DozerEntityMetadata<?> entityInformation) {
		ClassMappings classMappings = getClassMappings();

		boolean entityToAdaptedEntity = true;

		ClassMap mapping = StringUtils.isEmpty(entityInformation.getDozerMapId())
				? classMappings.find(entityInformation.getJavaType(), entityInformation.getAdaptedJavaType())
				: classMappings.find(entityInformation.getJavaType(), entityInformation.getAdaptedJavaType(),
						entityInformation.getDozerMapId());

		if (mapping == null) {
			mapping = StringUtils.isEmpty(entityInformation.getDozerMapId())
					? classMappings.find(entityInformation.getAdaptedJavaType(), entityInformation.getJavaType())
					: classMappings.find(entityInformation.getAdaptedJavaType(), entityInformation.getJavaType(),
							entityInformation.getDozerMapId());

			if (mapping == null || (mapping != null && MappingDirection.ONE_WAY == mapping.getType())) {
				return null;
			}

			entityToAdaptedEntity = false;
		}

		Class<?> srcClass = mapping.getSrcClassToMap();
		Class<?> destClass = mapping.getDestClassToMap();

        Object cacheKey = CacheKeyFactory.createKey(destClass, srcClass);

		Map<String, String> result = new HashMap<String, String>();

		BeanContainer beanContainer = getBeanContainer();

		List<Class<?>> superSrcClasses = MappingUtils.getSuperClassesAndInterfaces(srcClass, beanContainer);
		List<Class<?>> superDestClasses = MappingUtils.getSuperClassesAndInterfaces(destClass, beanContainer);

		// add the actual classes to check for mappings between the original and the
		// opposite
		// super classes
		superSrcClasses.add(0, srcClass);
		superDestClasses.add(0, destClass);

		for (Class<?> superSrcClass : superSrcClasses) {
			for (Class<?> superDestClass : superDestClasses) {
				if (!(superSrcClass.equals(srcClass) && superDestClass.equals(destClass))) {
					ClassMap superMapping = classMappings.find(superSrcClass, superDestClass,
							entityInformation.getDozerMapId());
					if (superMapping != null) {
						processDozerFieldMapping(entityToAdaptedEntity, superMapping, result);
					}
				}
			}
		}

		processDozerFieldMapping(entityToAdaptedEntity, mapping, result);

		return result;
	}

	protected void processDozerFieldMapping(boolean entityToAdaptedEntity, ClassMap mapping,
			Map<String, String> dozerEntityFieldNameToAdaptedFieldName) {

		for (FieldMap fieldMap : mapping.getFieldMaps()) {
			String entityFieldName = entityToAdaptedEntity ? fieldMap.getSrcFieldName() : fieldMap.getDestFieldName();
			String adaptedEntityFieldName = entityToAdaptedEntity ? fieldMap.getDestFieldName()
					: fieldMap.getSrcFieldName();

			if (!StringUtils.isEmpty(entityFieldName) && !StringUtils.isEmpty(adaptedEntityFieldName)
					&& !dozerEntityFieldNameToAdaptedFieldName.containsKey(entityFieldName)) {
				dozerEntityFieldNameToAdaptedFieldName.put(entityFieldName, adaptedEntityFieldName);
			}
		}
	}

	/**
	 * check if dozer can convert object of srcClass to object of destClass
	 * 
	 * @param srcClass  the source class
	 * @param destClass the destination class
	 * @param mapId     the mapping id to use
	 * 
	 * @return true if there is a mapping or converter, false otherwise
	 */
	public boolean hasDozerMapping(Class<?> srcClass, Class<?> destClass, String mapId) {
		ClassMap classMap = getClassMap(srcClass, destClass, mapId);

		if (classMap != null) {
			return true;
		}

		Configuration configuration = getGlobalConfiguration();

		if (configuration.getCustomConverters() != null
				&& configuration.getCustomConverters().findConverter(srcClass, destClass) != null) {
			return true;
		}

		return false;
	}

	protected ClassMap getClassMap(Class<?> srcClass, Class<?> destClass, String mapId) {
		ClassMappings classMappings = getClassMappings();

		ClassMap mapping = classMappings.find(srcClass, destClass, mapId);

		if (mapping == null) {
			mapping = classMappings.find(destClass, srcClass, mapId);
			if (mapping != null && MappingDirection.ONE_WAY == mapping.getType()) {
				return null;
			} else {
				return null;
			}
		}

		return mapping;
	}

	protected Configuration getGlobalConfiguration() {
		Field globalConfigurationField = ReflectionUtils.findField(dozerMapper.getMapperModelContext().getClass(),
				"globalConfiguration", Configuration.class);
		ReflectionUtils.makeAccessible(globalConfigurationField);
		Configuration globalConfiguration = (Configuration) ReflectionUtils.getField(globalConfigurationField,
				dozerMapper.getMapperModelContext());
		return globalConfiguration;
	}

	protected BeanContainer getBeanContainer() {
		Field beanContainerField = ReflectionUtils.findField(dozerMapper.getMapperModelContext().getClass(),
				"beanContainer", BeanContainer.class);
		ReflectionUtils.makeAccessible(beanContainerField);
		BeanContainer beanContainer = (BeanContainer) ReflectionUtils.getField(beanContainerField,
				dozerMapper.getMapperModelContext());
		return beanContainer;
	}

	protected ClassMappings getClassMappings() {
		Field classMappingsField = ReflectionUtils.findField(dozerMapper.getMappingMetadata().getClass(),
				"classMappings", ClassMappings.class);
		ReflectionUtils.makeAccessible(classMappingsField);
		ClassMappings classMappings = (ClassMappings) ReflectionUtils.getField(classMappingsField,
				dozerMapper.getMappingMetadata());
		return classMappings;
	}
}
