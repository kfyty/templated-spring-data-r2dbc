package com.kfyty.reactive.r2dbc.query.templated;

import com.kfyty.reactive.r2dbc.query.templated.utils.ReferenceUtil;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ReactiveExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.ReactiveExtensionAwareEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 重写 spel 表达式 root 对象
 *
 * @author kfyty725
 * @date 2023/7/9 19:23
 * @email kfyty725@hotmail.com
 */
public class TemplatedReactiveExtensionAwareQueryMethodEvaluationContextProvider extends ReactiveExtensionAwareQueryMethodEvaluationContextProvider {
    private final ReactiveExtensionAwareEvaluationContextProvider delegateReference;

    public TemplatedReactiveExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {
        super(beanFactory);
        this.delegateReference = ReferenceUtil.getReference("delegate", this, ReactiveExtensionAwareEvaluationContextProvider.class);
    }

    /**
     * 根对象改写为 Map，spel 表达式中使用 :#{[key].prop} 引用
     */
    @Override
    public <T extends Parameters<?, ?>> Mono<EvaluationContext> getEvaluationContextLater(T parameters,
                                                                                          Object[] parameterValues,
                                                                                          ExpressionDependencies dependencies) {
        Map<String, Object> rootObject = obtainParameterMap(parameters.getBindableParameters(), parameterValues);
        Mono<StandardEvaluationContext> evaluationContext = this.delegateReference.getEvaluationContextLater(rootObject, dependencies);

        return evaluationContext
                .doOnNext(it -> it.setVariables(collectVariables(parameters, parameterValues)))
                .cast(EvaluationContext.class);
    }

    /**
     * 获取参数 Map
     */
    public static Map<String, Object> obtainParameterMap(Parameters<?, ?> bindableParameters, Object[] parameterValues) {
        if (parameterValues.length < 1) {
            return Collections.emptyMap();
        }
        Map<String, Object> parameterMap = new HashMap<>();
        for (Parameter bindableParameter : bindableParameters) {
            parameterMap.put(bindableParameter.getName().get(), parameterValues[bindableParameter.getIndex()]);
        }
        return parameterMap;
    }

    /**
     * 复制 {@link ExtensionAwareQueryMethodEvaluationContextProvider#collectVariables(Parameters, Object[])}
     */
    private static Map<String, Object> collectVariables(Parameters<?, ?> parameters, Object[] arguments) {

        Map<String, Object> variables = new HashMap<>();

        parameters.stream()
                .filter(Parameter::isSpecialParameter)
                .forEach(it -> variables.put(
                                StringUtils.uncapitalize(it.getType().getSimpleName()),
                                arguments[it.getIndex()]
                        )
                );

        parameters.stream()
                .filter(Parameter::isNamedParameter)
                .forEach(it -> variables.put(
                                it.getName().orElseThrow(() -> new IllegalStateException("Should never occur")), //
                                arguments[it.getIndex()]
                        )
                );

        return variables;
    }
}
