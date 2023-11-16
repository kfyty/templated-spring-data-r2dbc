package com.kfyty.reactive.r2dbc.query.templated.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 描述: 分页
 *
 * @author kfyty725
 * @date 2023/7/9 18:51
 * @email kfyty725@hotmail.com
 */
@Data
@NoArgsConstructor
public class Pageable<T> {
    /**
     * 分页总数模板
     */
    public static final String COUNT_TEMPLATE = "select count(*) from (%s) t";

    /**
     * 分页参数模板
     */
    public static final String PAGE_TEMPLATE = " limit :#{[%s].offset}, :#{[%s].pageSize}";

    /**
     * 页码，从 0 开始
     */
    private int page;

    /**
     * 分页大小
     */
    private int pageSize;

    /**
     * 排序
     */
    private List<Order> orders;

    /**
     * 总数，查询自动返回
     */
    private long total;

    /**
     * 数据记录，查询自动返回
     */
    protected List<T> records;

    public Pageable(int page, int pageSize) {
        this(page, pageSize, Collections.emptyList());
    }

    public Pageable(int page, int pageSize, List<Order> orders) {
        this.page = page;
        this.pageSize = pageSize;
        this.orders = orders;
        this.records = new ArrayList<>();
    }

    /**
     * 获取分页偏移量
     */
    public long getOffset() {
        return this.page * (long) this.pageSize;
    }

    /**
     * 构造一个分页参数
     *
     * @param page     起始页码
     * @param pageSize 分页大小
     * @return 分页
     */
    public static <T> Pageable<T> of(int page, int pageSize) {
        return new Pageable<>(page, pageSize);
    }

    /**
     * 将分页对象和 Flux 数据打包合并为一个 Mono 分页数据
     *
     * @param page 分页对象
     * @param data 数据流
     * @return Mono 分页数据
     */
    public static <T> Mono<Pageable<T>> pack(Pageable<T> page, Flux<T> data) {
        return data.reduce(page, (e1, e2) -> {
            e1.getRecords().add(e2);
            return e1;
        }).doOnNext(e -> page.setOrders(null));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        /**
         * 排序项
         */
        private String item;

        /**
         * 排序顺序
         */
        private String order;
    }
}
