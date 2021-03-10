package com.rainlf.shardingshpere.shardingdatabaseexample.sharding;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;

/**
 * @author : rain
 * @date : 2021/3/10 15:43
 */
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
