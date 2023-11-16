package com.kfyty.reactive.r2dbc.query.templated.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * 描述: 属性配置
 *
 * @author kfyty725
 * @date 2023/7/9 0:33
 * @email kfyty725@hotmail.com
 */
@Data
public class TemplatedR2dbcProperties {
    /**
     * 模板路径
     */
    @Value("${spring.r2dbc.templated.path:}")
    private List<String> path;
}
