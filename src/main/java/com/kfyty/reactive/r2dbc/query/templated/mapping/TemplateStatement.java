package com.kfyty.reactive.r2dbc.query.templated.mapping;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 描述: 动态 SQL 模板
 *
 * @author kfyty725
 * @date 2021/9/28 13:06
 * @email kfyty725@hotmail.com
 */
@Data
@Accessors(chain = true)
public abstract class TemplateStatement {
    /**
     * 命令空间
     */
    public static final String TEMPLATE_NAMESPACE = "namespace";

    /**
     * statement id
     */
    public static final String TEMPLATE_STATEMENT_ID = "id";

    /**
     * 查询类标签
     */
    public static final String SELECT_LABEL = "select";

    /**
     * 执行类标签
     */
    public static final String EXECUTE_LABEL = "execute";

    /**
     * sql statement id
     */
    private String id;

    /**
     * sql type
     *
     * @see TemplateStatement#SELECT_LABEL
     * @see TemplateStatement#EXECUTE_LABEL
     */
    private String labelType;

    public TemplateStatement(String id, String labelType) {
        this.id = id;
        this.labelType = labelType;
    }
}
