package com.kfyty.reactive.r2dbc.query.templated;

import com.kfyty.reactive.r2dbc.query.templated.domain.Pageable;
import com.kfyty.reactive.r2dbc.query.templated.page.PageHandler;
import com.kfyty.reactive.r2dbc.query.templated.utils.ReferenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery;
import org.springframework.data.relational.repository.query.RelationalParameters;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.expression.ExpressionParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.kfyty.reactive.r2dbc.query.templated.TemplatedReactiveExtensionAwareQueryMethodEvaluationContextProvider.obtainParameterMap;

/**
 * 描述: SQL 模板支持
 *
 * @author kfyty725
 * @date 2023/7/9 10:19
 * @email kfyty725@hotmail.com
 */
@Slf4j
public class TemplatedTemplateBasedR2dbcQuery extends StringBasedR2dbcQuery {
    /**
     * 是否正在执行
     * spring 初始化实例为 false，每次执行语句都创建新的执行
     */
    private final boolean isRuntime;

    /**
     * 接口方法
     */
    private final Method method;

    /**
     * 动态模板 SQL 提供者
     */
    private final DynamicTemplateProvider<?> dynamicTemplateProvider;

    /**
     * 分页处理器
     */
    private final PageHandler pageHandler;

    /**
     * {@link R2dbcQueryMethod} 生产者
     */
    private final Supplier<R2dbcQueryMethod> queryMethodProducer;

    /**
     * 当前执行的查询语句
     */
    private final String query;

    /**
     * {@link R2dbcQueryMethod}
     */
    private final R2dbcQueryMethod queryMethod;

    /**
     * {@link R2dbcEntityOperations}
     */
    private final R2dbcEntityOperations entityOperations;

    /**
     * {@link R2dbcConverter}
     */
    private final R2dbcConverter converter;

    /**
     * {@link ReactiveDataAccessStrategy}
     */
    private final ReactiveDataAccessStrategy dataAccessStrategy;

    /**
     * {@link ExpressionParser}
     */
    private final ExpressionParser expressionParser;

    /**
     * {@link ReactiveQueryMethodEvaluationContextProvider}
     */
    private final ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;

    public TemplatedTemplateBasedR2dbcQuery(boolean isRuntime,
                                            Method method,
                                            DynamicTemplateProvider<?> dynamicTemplateProvider,
                                            PageHandler pageHandler,
                                            Supplier<R2dbcQueryMethod> queryMethodProducer,
                                            String query,
                                            R2dbcQueryMethod queryMethod,
                                            R2dbcEntityOperations entityOperations,
                                            R2dbcConverter converter,
                                            ReactiveDataAccessStrategy dataAccessStrategy,
                                            ExpressionParser expressionParser,
                                            ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider) {
        super(query, queryMethod, entityOperations, converter, dataAccessStrategy, expressionParser, evaluationContextProvider);
        this.isRuntime = isRuntime;
        this.method = method;
        this.dynamicTemplateProvider = dynamicTemplateProvider;
        this.pageHandler = pageHandler;
        this.queryMethodProducer = queryMethodProducer;
        this.query = query;
        this.queryMethod = queryMethod;
        this.entityOperations = entityOperations;
        this.converter = converter;
        this.dataAccessStrategy = dataAccessStrategy;
        this.expressionParser = expressionParser;
        this.evaluationContextProvider = evaluationContextProvider;
    }

    /**
     * 执行查询，如果第一个参数为 {@link Pageable}，则执行分页逻辑
     * 由于框架限制，方法有参数时一定会进行参数绑定，而执行动态 SQL 查询时有些参数并不需要，因此会报错
     * 所以在执行动态 SQL 查询时需先移除未使用的参数
     * <p>
     * 另外由于 spring 的参数解析后的信息保存在 {@link this#queryMethod} 中，为了避免查询出错时无法还原，
     * 因此每次查询时使用 {@link this#queryMethodProducer} 构建新的 {@link this#queryMethod}
     *
     * @param parameters must not be {@literal null}.
     * @return 查询结果
     */
    @Override
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Object execute(Object[] parameters) {
        // 非基于动态模板的查询，走原逻辑
        if (!this.isTemplatedQuery()) {
            return super.execute(parameters);
        }

        // 第一次进入基于动态模板的查询
        if (!this.isRuntime) {
            String query = this.resolveTemplatedQuery(parameters);
            return new TemplatedTemplateBasedR2dbcQuery(
                    true, method, dynamicTemplateProvider, pageHandler, queryMethodProducer,
                    query, queryMethodProducer.get(), entityOperations, converter, dataAccessStrategy, expressionParser, evaluationContextProvider).execute(parameters);
        }

        // 不是分页查询，直接执行
        if (parameters.length == 0 || !(parameters[0] instanceof Pageable<?>)) {
            return super.execute(parameters);
        }

        // 分页自动处理，当前是分页，直接执行
        if (this.pageHandler.isPageQuery(this.query)) {
            return super.execute(parameters);
        }

        final Pageable<?> pageable = (Pageable<?>) parameters[0];
        final String queryCount = this.pageHandler.buildCountQuery(this.query);

        // 查询数量时，需先移除未使用的参数，否则参数绑定失败
        final List<RelationalParameters.RelationalParameter> list = this.obtainParameters();
        final List<RelationalParameters.RelationalParameter> bak = this.removeUnusedParameter(searchQueryParameters(queryCount), list);

        // 执行分页查询
        return new TemplatedTemplateBasedR2dbcQuery(
                true, method, dynamicTemplateProvider, pageHandler, queryMethodProducer,
                queryCount, queryMethod, entityOperations, converter, dataAccessStrategy, expressionParser, evaluationContextProvider)
                .executeCount(parameters)
                .doOnNext(e -> this.resetUnusedParameter(list, bak))
                .flatMapMany(total -> {
                    // 记录总数并查询数据
                    pageable.setTotal(total);
                    String queryRecord = this.pageHandler.buildPageableQuery(this.query, bak.get(0).getName().get(), pageable);
                    Publisher<?> publisher = (Publisher<?>) new TemplatedTemplateBasedR2dbcQuery(
                            true, method, dynamicTemplateProvider, pageHandler, queryMethodProducer,
                            queryRecord, queryMethod, entityOperations, converter, dataAccessStrategy, expressionParser, evaluationContextProvider)
                            .execute(parameters);
                    return publisher == null ? Flux.empty() : publisher;
                });
    }

    /**
     * 查询分页总数
     *
     * @param parameters 方法参数
     * @return 分页总数
     */
    protected Mono<Long> executeCount(Object[] parameters) {
        return new TemplatedR2dbcParameterAccessor(this.queryMethod, parameters)
                .resolveParameters()
                .flatMapMany(accessor -> createQuery(accessor).flatMapMany(operation -> this.entityOperations.query(operation, Long.class).one()))
                .next()
                .defaultIfEmpty(0L);
    }

    /**
     * 是否是动态模板查询
     *
     * @return true if dynamic template
     */
    protected boolean isTemplatedQuery() {
        return this.method.isAnnotationPresent(Templated.class);
    }

    /**
     * 获取动态模板 SQL
     *
     * @param parameters 方法参数
     * @return SQL
     */
    protected String resolveTemplatedQuery(Object[] parameters) {
        Map<String, Object> parameterMap = obtainParameterMap(this.queryMethod.getParameters(), parameters);
        String statementId = this.dynamicTemplateProvider.resolveTemplateStatementId(this.method);
        String query = this.dynamicTemplateProvider.renderTemplate(statementId, parameterMap);
        if (log.isDebugEnabled()) {
            log.debug("resolved templated SQL: {}", query);
        }
        return query;
    }

    /**
     * 搜索 SQL 查询参数
     *
     * @param query SQL
     * @return 参数及索引
     */
    public static List<String> searchQueryParameters(String query) {
        List<String> parameterBindings = new LinkedList<>();
        SpelQueryContext queryContext = SpelQueryContext.of((counter, expression) -> {
            parameterBindings.add(expression);
            return expression;
        }, String::concat);
        SpelQueryContext.SpelExtractor parsed = queryContext.parse(query);
        return parameterBindings;
    }

    /**
     * 获取参数列表
     */
    @SuppressWarnings("unchecked")
    private List<RelationalParameters.RelationalParameter> obtainParameters() {
        RelationalParameters parameters = this.queryMethod.getParameters().getBindableParameters();
        return ReferenceUtil.getReference("parameters", parameters, List.class);
    }

    /**
     * 移除未使用的参数
     *
     * @param used SQL 中使用的参数
     * @param list 原参数
     * @return 原参数备份
     */
    private List<RelationalParameters.RelationalParameter> removeUnusedParameter(List<String> used, List<RelationalParameters.RelationalParameter> list) {
        List<RelationalParameters.RelationalParameter> bak = new ArrayList<>(list);
        loop:
        for (Iterator<RelationalParameters.RelationalParameter> i = list.iterator(); i.hasNext(); ) {
            String name = i.next().getName().get();
            for (String usedName : used) {
                if (usedName.contains(name)) {              // 必须使用包含
                    continue loop;
                }
            }
            i.remove();
        }
        return bak;
    }

    /**
     * 还原未使用的参数
     *
     * @param list 原参数
     * @param bak  备份参数
     */
    private void resetUnusedParameter(List<RelationalParameters.RelationalParameter> list, List<RelationalParameters.RelationalParameter> bak) {
        list.clear();
        list.addAll(bak);
    }
}
