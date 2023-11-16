package com.kfyty.reactive.r2dbc.query.templated.page;

import com.kfyty.reactive.r2dbc.query.templated.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述: 分页处理器
 *
 * @author kfyty725
 * @date 2023/7/9 0:44
 * @email kfyty725@hotmail.com
 */
public interface PageHandler {
    /**
     * 由当前查询语句生成 count 查询
     *
     * @param query sql
     * @return count sql
     */
    String buildCountQuery(String query);

    /**
     * 由当前查询语句生成分页查询
     *
     * @param query         sql
     * @param pageParamName 接口方法的分页参数名称
     * @param pageable      分页参数
     * @return 分页 sql
     */
    <T> String buildPageableQuery(String query, String pageParamName, Pageable<T> pageable);

    /**
     * 判断是否是数量查询
     *
     * @param query sql
     * @return true if count query
     */
    boolean isPageQuery(String query);

    /**
     * 仅构建排序参数
     *
     * @param pageable 分页参数
     * @return order by 子语句
     */
    default <T> String buildOrderQuery(Pageable<T> pageable) {
        List<Pageable.Order> orders = pageable.getOrders();
        if (orders == null || orders.isEmpty()) {
            return "";
        }
        return orders.stream().map(e -> " " + e.getItem() + " " + e.getOrder()).collect(Collectors.joining(","));
    }
}
