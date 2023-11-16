package com.kfyty.reactive.r2dbc.query.templated.support.enjoy;

import com.jfinal.template.Template;
import com.kfyty.reactive.r2dbc.query.templated.mapping.TemplateStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 描述: enjoy 动态 SQL 模板
 *
 * @author kfyty725
 * @date 2023/7/9 14:37
 * @email kfyty725@hotmail.com
 */
@Getter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class EnjoyTemplateStatement extends TemplateStatement {
    private final Template template;

    public EnjoyTemplateStatement(String id, String labelType, Template template) {
        super(id, labelType);
        this.template = template;
    }
}
