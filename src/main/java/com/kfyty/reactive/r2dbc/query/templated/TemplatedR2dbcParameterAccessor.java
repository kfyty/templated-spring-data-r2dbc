package com.kfyty.reactive.r2dbc.query.templated;

import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.data.relational.repository.query.RelationalParametersParameterAccessor;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.ReactiveWrappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: 复制 {@link org.springframework.data.r2dbc.repository.query.R2dbcParameterAccessor}，使之可以访问
 *
 * @author kfyty725
 * @date 2023/7/10 17:53
 * @email kfyty725@hotmail.com
 */
public class TemplatedR2dbcParameterAccessor extends RelationalParametersParameterAccessor {
    private final Object[] values;
    private final R2dbcQueryMethod method;

    /**
     * Creates a new {@link TemplatedR2dbcParameterAccessor}.
     */
    public TemplatedR2dbcParameterAccessor(R2dbcQueryMethod method, Object... values) {

        super(method, values);

        this.values = values;
        this.method = method;
    }

    /* (non-Javadoc)
     * @see org.springframework.data.relational.repository.query.RelationalParametersParameterAccessor#getValues()
     */
    @Override
    public Object[] getValues() {

        Object[] result = new Object[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = getValue(i);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.springframework.data.repository.query.ParametersParameterAccessor#getBindableValue(int)
     */
    public Object getBindableValue(int index) {
        return getValue(getParameters().getBindableParameter(index).getIndex());
    }

    /**
     * Resolve parameters that were provided through reactive wrapper types. Flux is collected into a list, values from
     * Mono's are used directly.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public Mono<TemplatedR2dbcParameterAccessor> resolveParameters() {
        boolean hasReactiveWrapper = false;

        for (Object value : values) {
            if (value == null || !ReactiveWrappers.supports(value.getClass())) {
                continue;
            }
            hasReactiveWrapper = true;
            break;
        }

        if (!hasReactiveWrapper) {
            return Mono.just(this);
        }

        Object[] resolved = new Object[values.length];
        Map<Integer, Optional<?>> holder = new ConcurrentHashMap<>();
        List<Publisher<?>> publishers = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            Object value = resolved[i] = values[i];
            if (value == null || !ReactiveWrappers.supports(value.getClass())) {
                continue;
            }

            if (ReactiveWrappers.isSingleValueType(value.getClass())) {
                int index = i;
                publishers.add(
                        ReactiveWrapperConverters.toWrapper(value, Mono.class)
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty())
                                .doOnNext(it -> holder.put(index, (Optional<?>) it))
                );
            } else {
                int index = i;
                publishers.add(
                        ReactiveWrapperConverters.toWrapper(value, Flux.class)
                                .collectList()
                                .doOnNext(it -> holder.put(index, Optional.of(it)))
                );
            }
        }

        return Flux.merge(publishers).then().thenReturn(resolved).map(values -> {
            holder.forEach((index, v) -> values[index] = v.orElse(null));
            return new TemplatedR2dbcParameterAccessor(method, values);
        });
    }
}
