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

