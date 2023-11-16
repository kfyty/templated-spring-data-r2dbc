package com.kfyty.reactive.r2dbc.query.templated;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述: 标记为动态模板查询方法
 *
 * @author kfyty725
 * @date 2023/7/9 10:19
 * @email kfyty725@hotmail.com
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Templated {
    String TEMPLATE = "templated";
}
