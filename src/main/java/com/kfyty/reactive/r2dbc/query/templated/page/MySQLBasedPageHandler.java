package com.kfyty.reactive.r2dbc.query.templated.page;

import com.kfyty.reactive.r2dbc.query.templated.domain.Pageable;

/**
 * 描述: mysql 分页处理器
 *
 * @author kfyty725
 * @date 2023/7/9 0:44
 * @email kfyty725@hotmail.com
 */
public class MySQLBasedPageHandler implements PageHandler {
    /**
     * 分页总数模板
     */
    public static final String COUNT_TEMPLATE = "select count(*) from (%s) t";

    /**
     * 分页参数模板
     */
    public static final String PAGE_TEMPLATE = " limit :#{[%s].offset}, :#{[%s].pageSize}";

    @Override
    public String buildCountQuery(String query) {
        return String.format(COUNT_TEMPLATE, query);
    }

    @Override
    public <T> String buildPageableQuery(String query, String pageParamName, Pageable<T> pageable) {
        String order = this.buildOrderQuery(pageable);
        if (order == null || order.isEmpty()) {
            return query + String.format(PAGE_TEMPLATE, pageParamName, pageParamName);
        }
        boolean hasOrder = query.toLowerCase().contains("order by");
        String orderedQuery = query + (hasOrder ? "," + order : " order by " + order);
        return orderedQuery + String.format(PAGE_TEMPLATE, pageParamName, pageParamName);
    }

    /**
     * 走到这里说明是分页处理，因此如果不是分页查询，就一定是先进行的数量查询
     *
     * @see MySQLBasedPageHandler#PAGE_TEMPLATE
     */
    @Override
    public boolean isPageQuery(String query) {
        return query.endsWith("].pageSize}");
    }
}
