package com.rainlf.shardingshpere.shardingdatabaseexample;

import com.rainlf.shardingshpere.shardingdatabaseexample.dao.User;
import com.rainlf.shardingshpere.shardingdatabaseexample.dao.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Slf4j
@SpringBootTest
@SpringBootApplication
public class ShardingDatabaseExampleApplication {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void test() {
        log.info("delete all");
        userMapper.deleteAll();

        log.info("insert 2 a & 3 b");
        userMapper.insert(new User("a", UUID.randomUUID().toString().substring(0, 16)));
        userMapper.insert(new User("a", UUID.randomUUID().toString().substring(0, 16)));
        userMapper.insert(new User("b", UUID.randomUUID().toString().substring(0, 16)));
        userMapper.insert(new User("b", UUID.randomUUID().toString().substring(0, 16)));
        userMapper.insert(new User("b", UUID.randomUUID().toString().substring(0, 16)));

        log.info("select a");
        log.info("select a result: {}", userMapper.selectByTenant("a"));
        log.info("select b");
        log.info("select b result: {}", userMapper.selectByTenant("b"));
        log.info("select all");
        log.info("select all result: {}", userMapper.selectAll());
    }

}
