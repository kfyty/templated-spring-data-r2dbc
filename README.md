# templated-spring-data-r2dbc
    拓展 spring-data-r2dbc，支持用户自定义引入三方模板引擎来实现动态SQL，默认提供了 enjoy 模板实现。并封装了分页查询，可以一次调用返回总数以及分页记录。

下面是示例：
```xml
        <dependency>
            <groupId>com.kfyty</groupId>
            <artifactId>templated-spring-data-r2dbc</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
```java
@Repository
public interface UserDao extends R2dbcRepository<UserDO, Long> {
    @Templated
    @Query(Templated.TEMPLATE)
    Flux<UserDO> page(Pageable<UserDO> page, Long id, UserDO user);
}
```
```xml
<mapper namespace="com.kfyty.templated.r2dbc.dao.UserDao">
    <select id="page">
        <![CDATA[
        select
            id,
            open_id
        from
            user
        where 1 = 1
        #if(id != null)
            and id = :#{[id]}
        #end
        #if(user != null && user.nickname != null)
            and nickname = :#{[user].nickname}
        #end
        ]]>
    </select>
</mapper>
```

由于 r2dbc 默认使用 Object[] 传参，该项目重写为了使用 Map 传参，因此 SQL 中的参数引用接口中的参数时，
需先使用 #{[key]}， 然后再获取参数嵌套属性。对于 enjoy 模板则可以直接引用接口参数值。

单元测试如下：
```java
    @Autowired
    private UserDao userDao;

    @Test
    public void pageTest() {
        Pageable<UserDO> page = Pageable.of(0, 1);

        UserDO user = new UserDO();
        user.setNickname("test");

        // 由于 r2dbc 框架限制，方法接口只能返回 Flux<?> 类型，但提供了转换方法，可以转换为常见的 Mono<Pageable<UserDO>> 类型
        Flux<UserDO> flux = this.userDao.page(page, 1L, user);
        Mono<Pageable<UserDO>> pageUserDO = Pageable.pack(page, flux);

        pageUserDO.subscribe(System.out::println);
    }
```
