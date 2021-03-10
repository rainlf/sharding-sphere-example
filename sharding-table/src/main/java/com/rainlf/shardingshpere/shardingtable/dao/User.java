package com.rainlf.shardingshpere.shardingtable.dao;

import lombok.Data;

/**
 * @author : rain
 * @date : 2021/3/10 12:33
 */
@Data
public class User {
    private Integer id;
    private String tenant;
    private String name;

    public User() {
    }

    public User(String tenant, String name) {
        this.tenant = tenant;
        this.name = name;
    }
}
