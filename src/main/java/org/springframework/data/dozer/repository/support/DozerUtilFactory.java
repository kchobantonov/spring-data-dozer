package org.springframework.data.dozer.repository.support;

import com.github.dozermapper.core.Mapper;

public class DozerUtilFactory {
	private static DozerUtilFactory INSTANCE = new DozerUtilFactory();

	protected DozerUtilFactory() {
	}

	public static DozerUtilFactory getInstance() {
		return INSTANCE;
	}

	public static void setInstance(DozerUtilFactory factory) {
		INSTANCE = factory;
	}

	public DozerUtil getDozerUtil(Mapper dozerMapper) {
		return new DozerUtil(dozerMapper);
	}
}
