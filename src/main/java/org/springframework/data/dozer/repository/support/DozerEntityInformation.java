package org.springframework.data.dozer.repository.support;

import org.springframework.data.dozer.repository.query.DozerEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

public interface DozerEntityInformation<T, ID> extends EntityInformation<T, ID>, DozerEntityMetadata<T> {

}
