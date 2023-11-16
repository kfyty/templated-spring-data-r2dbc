package com.kfyty.reactive.r2dbc.query.templated.config;

import com.jfinal.template.Engine;
import com.kfyty.reactive.r2dbc.query.templated.DynamicTemplateProvider;
import com.kfyty.reactive.r2dbc.query.templated.page.MySQLBasedPageHandler;
import com.kfyty.reactive.r2dbc.query.templated.page.PageHandler;
import com.kfyty.reactive.r2dbc.query.templated.support.enjoy.EnjoyTemplatedProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述: 自动配置
 *
 * @author kfyty725
 * @date 2023/7/9 0:33
 * @email kfyty725@hotmail.com
 */
@Configuration
public class TemplatedR2dbcAutoConfig {

    @Bean
    public TemplatedR2dbcProperties templatedR2dbcProperties() {
        return new TemplatedR2dbcProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public PageHandler pageHandler() {
        return new MySQLBasedPageHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Engine.class)
    public DynamicTemplateProvider<?> dynamicTemplateProvider() {
        EnjoyTemplatedProvider enjoyTemplatedProvider = new EnjoyTemplatedProvider();
        enjoyTemplatedProvider.setTemplatePath(this.templatedR2dbcProperties().getPath());
        return enjoyTemplatedProvider;
    }
}
