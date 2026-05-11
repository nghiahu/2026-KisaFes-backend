package org.example.backend.common.base;

import java.util.List;

public interface BaseService<T extends BaseEntity, ID> {
    T save(T entity);

    T findById(ID id);

    List<T> findAll();

    void deleteById(ID id);
}
