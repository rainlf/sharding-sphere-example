package com.rainlf.shardingshpere.shardingtable.sharding;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;

/**
 * @author : rain
 * @date : 2021/3/10 12:43
 */
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
