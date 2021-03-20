# Sharding Sphere Example

`Apache ShardingSphere` 是一套开源的分布式数据库解决方案组成的生态圈，包含`JDBC`、`Proxy`和`Sidecar`三款产品，提供了标准化的数据水平扩展、分布式事务和分布式治理等功能。

这里以数据分片为背景，使用`sharding sphere jdbc` 来演示他的无侵入式的多租户实现方案。

## 环境

- `java 1.8`
- `springboot 2.4.3`
- `sharding sphere 4.1.1`

## 核心概念

在数据分片的背景下，`sharding sphere`中设计的核心概念有：

### 逻辑表

水平拆分的数据库（表）的相同逻辑和数据结构表的总称。例：订单数据根据主键尾数拆分为10张表，分别是`t_order_0`到`t_order_9`，他们的逻辑表名为`t_order`。

### 真实表

在分片的数据库中真实存在的物理表。即上个示例中的`t_order_0`到`t_order_9`。

### 数据节点

数据分片的最小单元。由数据源名称和数据表组成，例：`ds_0.t_order_0`。

### 绑定表

指分片规则一致的主表和子表。例如：`t_order`表和`t_order_item`表，均按照`order_id`分片，则此两张表互为绑定表关系。绑定表之间的多表关联查询不会出现笛卡尔积关联，关联查询效率将大大提升。举例说明，如果SQL为：

```sql
SELECT i.*
FROM t_order o
         JOIN t_order_item i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);
```

在不配置绑定表关系时，假设分片键order_id将数值10路由至第0片，将数值11路由至第1片，那么路由后的SQL应该为4条，它们呈现为笛卡尔积:

```sql
SELECT i.*
FROM t_order_0 o
         JOIN t_order_item_0 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);

SELECT i.*
FROM t_order_0 o
         JOIN t_order_item_1 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);

SELECT i.*
FROM t_order_1 o
         JOIN t_order_item_0 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);

SELECT i.*
FROM t_order_1 o
         JOIN t_order_item_1 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);
```

在配置绑定表关系后，路由的SQL应该为2条

```sql
SELECT i.*
FROM t_order_0 o
         JOIN t_order_item_0 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);

SELECT i.*
FROM t_order_1 o
         JOIN t_order_item_1 i ON o.order_id = i.order_id
WHERE o.order_id in (10, 11);
```

其中t_order在FROM的最左侧，ShardingSphere将会以它作为整个绑定表的主表。 所有路由计算将会只使用主表的策略，那么t_order_item表的分片计算将会使用t_order的条件。故绑定表之间的分区键要完全相同。

### 广播表

指所有的分片数据源中都存在的表，表结构和表中的数据在每个数据库中均完全一致。适用于数据量不大且需要与海量数据的表进行关联查询的场景，例如：字典表。

## 项目示例

### 依赖
```xml
<!-- for spring boot -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
    <version>${shardingsphere.version}</version>
</dependency>
<!-- for spring namespace -->
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>sharding-jdbc-spring-namespace</artifactId>
    <version>${shardingsphere.version}</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.5</version>
</dependency>
<!-- mysql -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 分表多租户方案

以这种方式实现的数据库多租户方案，不同租户间共享数据库实例与`schema`，隔离程度较低。下文详细说明配置和实现。

初始化数据库，这里建立了`t_user_a`和`t_user_b`两张表，这里的表是真实表
```sql
create
database if not exists sharding_sphere_db0;

use
sharding_sphere_db0;
drop table if exists t_user_a;
drop table if exists t_user_b;

create table t_user_a
(
    id     int auto_increment primary key,
    tenant varchar(32) null,
    name   varchar(32) null
);

create table t_user_b
(
    id     int auto_increment primary key,
    tenant varchar(32) null,
    name   varchar(32) null
);
```
而在程序中则直接操作的是逻辑表`t_user`如下
```java
@Mapper
public interface UserMapper {

    @Insert("insert into t_user (tenant, name) values(#{tenant}, #{name})")
    void insert(User user);

    @Select("select id, tenant, name from t_user where tenant = #{tenant}")
    List<User> selectByTenant(@Param("tenant") String tenant);

    @Select("select id, tenant, name from t_user")
    List<User> selectAll();

    @Delete("delete from t_user")
    void deleteAll();
}
```
使用时在配置文件中指定数据源`db0`，真实节点`db0.t_user_a`、`db0_t_user_b`，同时指定分表键`tenant`
```properties
spring.shardingsphere.datasource.names=db0
spring.shardingsphere.datasource.db0.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.db0.driver-class-name=com.mysql.cj.jdbc.Driver
spring.shardingsphere.datasource.db0.url=jdbc:mysql://localhost:3306/sharding_sphere_db0?useUnicode=true&characterEncoding=utf8
spring.shardingsphere.datasource.db0.username=root
spring.shardingsphere.datasource.db0.password=root
## logic table: t_user
spring.shardingsphere.sharding.tables.t_user.actual-data-nodes=db0.t_user_a,db0.t_user_b
spring.shardingsphere.sharding.tables.t_user.table-strategy.standard.sharding-column=tenant
spring.shardingsphere.sharding.tables.t_user.table-strategy.standard.precise-algorithm-class-name=com.rainlf.shardingshpere.shardingtableexample.sharding.TUserShardingAlgorithm
spring.shardingsphere.props.sql.show=true
```
以及自定义分表算法
```java
public class TUserShardingAlgorithm implements PreciseShardingAlgorithm<String> {

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        String targetTable = "t_user_" + preciseShardingValue.getValue();
        if (collection.contains(targetTable)) {
            return targetTable;
        }

        throw new UnsupportedOperationException("无法判定的分区键: " + preciseShardingValue.getValue());
    }
}
```
启动应用后运行，调用相应接口，运行SQL
```sql
insert into t_user (tenant, name) values("a", "I am a");
insert into t_user (tenant, name) values("b", "I am b");
```
可以观察到，SQL语句会被路由至
```sql
insert into t_user_a (tenant, name) values("a", "I am a");
insert into t_user_b (tenant, name) values("b", "I am b");
```
实现自动分片策略，查询更新删除等操作有同样效果

### 分库多租户方案

以这种方式实现的数据库多租户方案，不同租户间通过不同数据库实例或`schema`隔离，隔离程度较高。下文详细说明配置和实现。

初始化数据库，这里建立了`sharding_sphere_db1`和`sharding_sphere_db2`两个`schema`，并分别在数据库中建立`t_user`表
```sql
create database if not exists sharding_sphere_db1;
create database if not exists sharding_sphere_db2;

use sharding_sphere_db1;
drop table if exists t_user;
create table t_user
(
    id     int auto_increment primary key,
    tenant varchar(32) null,
    name   varchar(32) null
);

use sharding_sphere_db2;
drop table if exists t_user;
create table t_user
(
    id     int auto_increment primary key,
    tenant varchar(32) null,
    name   varchar(32) null
);

```
程序中不关心具体的`t_user`在哪个数据源中
```java
@Mapper
public interface UserMapper {

    @Insert("insert into t_user (tenant, name) values(#{tenant}, #{name})")
    void insert(User user);

    @Select("select id, tenant, name from t_user where tenant = #{tenant}")
    List<User> selectByTenant(@Param("tenant") String tenant);

    @Select("select id, tenant, name from t_user")
    List<User> selectAll();

    @Delete("delete from t_user")
    void deleteAll();
}
```
在配置文件中指定数据源`db1`和`db2`，真实节点`db1.t_user`、`db2_t_user`，同时指定分库键`tenant`
```properties
spring.shardingsphere.datasource.names=db1,db2
spring.shardingsphere.datasource.db1.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.db1.driver-class-name=com.mysql.cj.jdbc.Driver
spring.shardingsphere.datasource.db1.url=jdbc:mysql://localhost:3306/sharding_sphere_db1?useUnicode=true&characterEncoding=utf8
spring.shardingsphere.datasource.db1.username=root
spring.shardingsphere.datasource.db1.password=root
spring.shardingsphere.datasource.db2.type=com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.db2.driver-class-name=com.mysql.cj.jdbc.Driver
spring.shardingsphere.datasource.db2.url=jdbc:mysql://localhost:3306/sharding_sphere_db2?useUnicode=true&characterEncoding=utf8
spring.shardingsphere.datasource.db2.username=root
spring.shardingsphere.datasource.db2.password=root
## logic table: t_user
spring.shardingsphere.sharding.default-database-strategy.standard.sharding-column=tenant
spring.shardingsphere.sharding.default-database-strategy.standard.precise-algorithm-class-name=com.rainlf.shardingshpere.shardingdatabaseexample.sharding.DatabaseShardingAlgorithm
spring.shardingsphere.sharding.tables.t_user.actual-data-nodes=db1.t_user,db2.t_user
spring.shardingsphere.props.sql.show=true

```
以及自定义分库算法
```java
public class DatabaseShardingAlgorithm implements PreciseShardingAlgorithm<String> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        String targetTable = "db" + getDbPostfix(preciseShardingValue.getValue());
        if (collection.contains(targetTable)) {
            return targetTable;
        }

        throw new UnsupportedOperationException("无法判定的分区键: " + preciseShardingValue.getValue());
    }

    public String getDbPostfix(String tenant) {
        switch (tenant) {
            case "a":
                return "1";
            case "b":
                return "2";
            default:
                return "";
        }
    }
}
```
启动应用后运行，调用相应接口，运行SQL
```sql
insert into t_user (tenant, name) values("a", "I am a");
insert into t_user (tenant, name) values("b", "I am b");
```
可以观察到，SQL语句会被路由至
```sql
insert into sharding_sphere_db1.t_user_a (tenant, name) values("a", "I am a");
insert into sharding_sphere_db2.t_user_b (tenant, name) values("b", "I am b");
```
实现数据的自动分片策略，查询更新删除等操作有同样效果，达到多租户的目的。



