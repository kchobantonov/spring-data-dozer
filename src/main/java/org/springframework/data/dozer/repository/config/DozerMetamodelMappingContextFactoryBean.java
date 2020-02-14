package org.springframework.data.dozer.repository.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.dozer.mapping.DozerMetamodelMappingContext;
import org.springframework.lang.Nullable;

public class DozerMetamodelMappingContextFactoryBean extends AbstractFactoryBean<DozerMetamodelMappingContext>
		implements ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(DozerMetamodelMappingContextFactoryBean.class);

	private @Nullable ListableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.context.ApplicationContextAware#setApplicationContext(org
	 * .springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return DozerMetamodelMappingContext.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	@Override
	protected DozerMetamodelMappingContext createInstance() throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Initializing DozerMetamodelMappingContext¦");
		}

		DozerMetamodelMappingContext context = new DozerMetamodelMappingContext();
		context.initialize();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Finished initializing DozerMetamodelMappingContext!");
		}

		return context;
	}

}
