package com.rainlf.shardingshpere.shardingdatabaseexample.dao;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author : rain
 * @date : 2021/3/10 12:34
 */
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
