package org.example.backend.common.base;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

@RequiredArgsConstructor
public abstract class BaseServiceImpl<T extends BaseEntity, ID> implements BaseService<T, ID> {

    private final MongoRepository<T, ID> repository;

    @Override
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    public T findById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(ID id) {
        if (!repository.existsById(id)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
