# Sharding Sphere Example

## 环境

- `java 1.8`
- `springboot 2.4.3`
- `sharding sphere 4.1.1`

## 项目

### sharding-table-example

以分表方式实现数据库多租户，不同租户间共享数据库实例与`schema`，隔离程度较低，具体规则参考项目内`application.properties`

### sharding-database-example

以分库的方式实现数据库多租户，不同租户间通过不同数据库实例或`schema`隔离，隔离程度较高，具体规则参考项目内`application.properties`



