package com.kfyty.reactive.r2dbc.query.templated.factory;

import com.kfyty.reactive.r2dbc.query.templated.DynamicTemplateProvider;
import com.kfyty.reactive.r2dbc.query.templated.TemplatedTemplateBasedR2dbcQuery;
import com.kfyty.reactive.r2dbc.query.templated.page.PageHandler;
import com.kfyty.reactive.r2dbc.query.templated.utils.ReferenceUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.query.PartTreeR2dbcQuery;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 描述: 重写 {@link R2dbcRepositoryFactory#getQueryLookupStrategy}
 *
 * @author kfyty725
 * @date 2023/7/9 0:44
 * @email kfyty725@hotmail.com
 */
public class TemplatedR2dbcRepositoryFactory extends R2dbcRepositoryFactory {
    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final ReactiveDataAccessStrategy dataAccessStrategyRef;
    private final R2dbcConverter converterRef;
    private final R2dbcEntityOperations operationsRef;

    /**
     * 动态模板 SQL 提供者
     */
    private DynamicTemplateProvider<?> dynamicTemplateProvider;

    /**
     * 分页处理器
     */
    private PageHandler pageHandler;

    public TemplatedR2dbcRepositoryFactory(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy) {
        super(databaseClient, dataAccessStrategy);
        this.dataAccessStrategyRef = dataAccessStrategy;
        this.converterRef = ReferenceUtil.getReference("converter", this, R2dbcConverter.class);
        this.operationsRef = ReferenceUtil.getReference("operations", this, R2dbcEntityOperations.class);
    }

    public TemplatedR2dbcRepositoryFactory(R2dbcEntityOperations operations) {
        super(operations);
        this.dataAccessStrategyRef = ReferenceUtil.getReference("dataAccessStrategy", this, ReactiveDataAccessStrategy.class);
        this.converterRef = ReferenceUtil.getReference("converter", this, R2dbcConverter.class);
        this.operationsRef = operations;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        super.setBeanFactory(beanFactory);
        if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
            if (listableBeanFactory.getBeanNamesForType(DynamicTemplateProvider.class).length > 0) {
                this.dynamicTemplateProvider = beanFactory.getBean(DynamicTemplateProvider.class);
            }
            if (listableBeanFactory.getBeanNamesForType(DynamicTemplateProvider.class).length > 0) {
                this.pageHandler = beanFactory.getBean(PageHandler.class);
            }
        }
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(
                new TemplatedR2dbcQueryLookupStrategy((ReactiveQueryMethodEvaluationContextProvider) evaluationContextProvider)
        );
    }

    private class TemplatedR2dbcQueryLookupStrategy implements QueryLookupStrategy {
        private final ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;
        private final ExpressionParser parser = EXPRESSION_PARSER;

        TemplatedR2dbcQueryLookupStrategy(ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider) {
            this.evaluationContextProvider = evaluationContextProvider;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
            R2dbcQueryMethod queryMethod = new R2dbcQueryMethod(method, metadata, factory, converterRef.getMappingContext());
            String namedQueryName = queryMethod.getNamedQueryName();

            if (namedQueries.hasQuery(namedQueryName)) {
                String namedQuery = namedQueries.getQuery(namedQueryName);
                return new StringBasedR2dbcQuery(
                        namedQuery,
                        queryMethod,
                        operationsRef,
                        converterRef,
                        dataAccessStrategyRef,
                        this.parser,
                        this.evaluationContextProvider
                );
            }
            if (queryMethod.hasAnnotatedQuery()) {
                return new TemplatedTemplateBasedR2dbcQuery(
                        false,
                        method,
                        dynamicTemplateProvider,
                        pageHandler,
                        () -> new R2dbcQueryMethod(method, metadata, factory, converterRef.getMappingContext()),
                        queryMethod.getRequiredAnnotatedQuery(),
                        queryMethod,
                        operationsRef,
                        converterRef,
                        dataAccessStrategyRef,
                        this.parser,
                        this.evaluationContextProvider
                );
            }
            return new PartTreeR2dbcQuery(
                    queryMethod,
                    operationsRef,
                    converterRef,
                    dataAccessStrategyRef
            );
        }
    }
}
