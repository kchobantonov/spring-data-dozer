package org.springframework.data.dozer.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface DozerRepository<T, ID> extends PagingAndSortingRepository<T, ID> {

}
