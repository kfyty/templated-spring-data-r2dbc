package com.kfyty.reactive.r2dbc.query.templated.factory;

import com.kfyty.reactive.r2dbc.query.templated.TemplatedReactiveExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.r2dbc.core.DatabaseClient;

import java.io.Serializable;
import java.util.Optional;

/**
 * 描述: {@link R2dbcRepositoryFactoryBean}，重写返回 {@link R2dbcRepositoryFactoryBean#getFactoryInstance} 方法返回值
 * <p>
 * 用于 {@link org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories#repositoryFactoryBeanClass}，以启用模板支持
 *
 * @author kfyty725
 * @date 2023/7/9 0:33
 * @email kfyty725@hotmail.com
 */
public class TemplatedR2dbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends R2dbcRepositoryFactoryBean<T, S, ID> {
    /**
     * Creates a new {@link TemplatedR2dbcRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public TemplatedR2dbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected Optional<QueryMethodEvaluationContextProvider> createDefaultQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {
        return Optional.of(new TemplatedReactiveExtensionAwareQueryMethodEvaluationContextProvider(beanFactory));
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(DatabaseClient client, ReactiveDataAccessStrategy dataAccessStrategy) {
        return new TemplatedR2dbcRepositoryFactory(client, dataAccessStrategy);
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(R2dbcEntityOperations operations) {
        return new TemplatedR2dbcRepositoryFactory(operations);
    }
}
