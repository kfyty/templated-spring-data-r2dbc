package com.kfyty.reactive.r2dbc.query.templated.utils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * 描述: 基于反射的引用获取工具
 *
 * @author kfyty725
 * @date 2023/7/9 0:44
 * @email kfyty725@hotmail.com
 */
public abstract class ReferenceUtil {

    @SuppressWarnings("unchecked")
    public static <T> T getReference(String name, Object target, Class<T> referenceType) {
        Field field = ReflectionUtils.findField(target.getClass(), name);
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, target);
    }
}
