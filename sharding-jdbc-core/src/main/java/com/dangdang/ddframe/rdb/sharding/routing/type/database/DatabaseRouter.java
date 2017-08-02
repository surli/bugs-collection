/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.routing.type.database;

import com.dangdang.ddframe.rdb.sharding.api.ShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.DatabaseShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.hint.HintManagerHolder;
import com.dangdang.ddframe.rdb.sharding.hint.ShardingKey;
import com.dangdang.ddframe.rdb.sharding.constant.SQLType;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;

/**
 * 库路由.
 * 
 * @author gaohongtao
 */
@RequiredArgsConstructor
@Slf4j
public final class DatabaseRouter {
    
    private final DataSourceRule dataSourceRule;
    
    private final DatabaseShardingStrategy databaseShardingStrategy;
    
    private final SQLType sqlType;
    
    /**
     * 根据线索路由到库.
     * 
     * @return 库路由结果
     */
    public DatabaseRoutingResult route() {
        Optional<ShardingValue<?>> shardingValue = HintManagerHolder.getDatabaseShardingValue(new ShardingKey(HintManagerHolder.DB_TABLE_NAME, HintManagerHolder.DB_COLUMN_NAME));
        Preconditions.checkState(shardingValue.isPresent());
        log.debug("Before database sharding only db:{} sharding values: {}", dataSourceRule.getDataSourceNames(), shardingValue.get());
        Collection<String> result = databaseShardingStrategy.doStaticSharding(sqlType, dataSourceRule.getDataSourceNames(), Collections.<ShardingValue<?>>singleton(shardingValue.get()));
        Preconditions.checkState(!result.isEmpty(), "no database route info");
        log.debug("After database sharding only result: {}", result);
        return new DatabaseRoutingResult(result);
    }
}
