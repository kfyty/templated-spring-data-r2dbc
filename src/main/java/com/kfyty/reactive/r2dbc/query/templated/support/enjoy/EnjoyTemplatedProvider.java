package com.kfyty.reactive.r2dbc.query.templated.support.enjoy;

import com.jfinal.template.Engine;
import com.kfyty.reactive.r2dbc.query.templated.AbstractDynamicTemplateProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 描述: 加载 enjoy 模板
 *
 * @author kfyty725
 * @date 2023/7/9 14:25
 * @email kfyty725@hotmail.com
 */
@Slf4j
public class EnjoyTemplatedProvider extends AbstractDynamicTemplateProvider<EnjoyTemplateStatement> {
    /**
     * enjoy 引擎
     */
    private Engine engine;

    @Override
    public String renderTemplate(String statementId, Map<String, Object> params) {
        EnjoyTemplateStatement template = this.templateStatements.get(statementId);
        String sql = template.getTemplate().renderToString(params);
        return sql.replaceAll(BLANK_LINE_PATTERN.pattern(), "").trim();
    }

    @Override
    protected EnjoyTemplateStatement resolveInternal(String namespace, String id, String labelType, String content) {
        return new EnjoyTemplateStatement(id, labelType, this.engine.getTemplateByString(content));
    }

    @Override
    public void afterPropertiesSet() {
        this.engine = Engine.create(this.getClass().getSimpleName());
        super.afterPropertiesSet();
    }
}
