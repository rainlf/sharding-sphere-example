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
