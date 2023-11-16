package com.kfyty.reactive.r2dbc.query.templated;

import com.kfyty.reactive.r2dbc.query.templated.mapping.TemplateStatement;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 描述: 动态模板 SQL 提供者
 *
 * @author kfyty725
 * @date 2023/7/9 14:31
 * @email kfyty725@hotmail.com
 */
public interface DynamicTemplateProvider<TS extends TemplateStatement> {
    /**
     * 设置模板路径
     *
     * @param paths 路径
     */
    void setTemplatePath(List<String> paths);

    /**
     * 根据给出的路径，解析出动态 SQL 模板集合
     *
     * @param paths 路径
     * @return TemplateStatement
     */
    List<TS> resolve(List<String> paths);

    /**
     * 根据给定的接口方法解析动态 SQL id
     *
     * @param method 方法
     * @return id
     */
    String resolveTemplateStatementId(Method method);

    /**
     * 渲染给定的动态 SQL 模板
     *
     * @param statementId 模板 id
     * @param params      模板参数
     * @return 渲染后的 SQL
     */
    String renderTemplate(String statementId, Map<String, Object> params);
}
