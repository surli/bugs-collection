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

package com.dangdang.ddframe.rdb.sharding.jdbc;

import com.dangdang.ddframe.rdb.sharding.executor.PreparedStatementExecutor;
import com.dangdang.ddframe.rdb.sharding.executor.wrapper.PreparedStatementExecutorWrapper;
import com.dangdang.ddframe.rdb.sharding.jdbc.adapter.AbstractPreparedStatementAdapter;
import com.dangdang.ddframe.rdb.sharding.merger.ResultSetFactory;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.GeneratedKeyContext;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.InsertSQLContext;
import com.dangdang.ddframe.rdb.sharding.routing.PreparedStatementRoutingEngine;
import com.dangdang.ddframe.rdb.sharding.routing.SQLExecutionUnit;
import com.dangdang.ddframe.rdb.sharding.routing.SQLRouteResult;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 支持分片的预编译语句对象.
 * 
 * @author zhangliang
 * @author caohao
 */
public final class ShardingPreparedStatement extends AbstractPreparedStatementAdapter {
    
    private final PreparedStatementRoutingEngine preparedStatementRoutingEngine;
    
    private final List<PreparedStatementExecutorWrapper> cachedPreparedStatementWrappers = new ArrayList<>();
    
    private Integer autoGeneratedKeys;
    
    private int[] columnIndexes;
    
    private String[] columnNames;
    
    private int batchIndex;
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, final String sql) {
        this(shardingConnection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, 
            final String sql, final int resultSetType, final int resultSetConcurrency) {
        this(shardingConnection, sql, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, 
            final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        super(shardingConnection, resultSetType, resultSetConcurrency, resultSetHoldability);
        preparedStatementRoutingEngine = new PreparedStatementRoutingEngine(sql, shardingConnection.getShardingContext());
    }
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, final String sql, final int autoGeneratedKeys) {
        this(shardingConnection, sql);
        this.autoGeneratedKeys = autoGeneratedKeys;
    }
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, final String sql, final int[] columnIndexes) {
        this(shardingConnection, sql);
        this.columnIndexes = columnIndexes;
    }
    
    ShardingPreparedStatement(final ShardingConnection shardingConnection, final String sql, final String[] columnNames) {
        this(shardingConnection, sql);
        this.columnNames = columnNames;
    }
    
    @Override
    public ResultSet executeQuery() throws SQLException {
        ResultSet rs;
        try {
            rs = ResultSetFactory.getResultSet(
                    new PreparedStatementExecutor(getShardingConnection().getShardingContext().getExecutorEngine(), routeSQL()).executeQuery(), getSqlContext());
        } finally {
            clearRouteContext();
        }
        setCurrentResultSet(rs);
        return rs;
    }
    
    @Override
    public int executeUpdate() throws SQLException {
        try {
            return new PreparedStatementExecutor(getShardingConnection().getShardingContext().getExecutorEngine(), routeSQL()).executeUpdate();
        } finally {
            clearRouteContext();
        }
    }
    
    @Override
    public boolean execute() throws SQLException {
        try {
            return new PreparedStatementExecutor(getShardingConnection().getShardingContext().getExecutorEngine(), routeSQL()).execute();
        } finally {
            clearRouteContext();
        }
    }
    
    protected void clearRouteContext() throws SQLException {
        resetBatch();
        cachedPreparedStatementWrappers.clear();
        batchIndex = 0;
    }
    
    @Override
    public void clearBatch() throws SQLException {
        clearRouteContext();
    }
    
    @Override
    public void addBatch() throws SQLException {
        try {
            for (PreparedStatementExecutorWrapper each : routeSQL()) {
                each.getPreparedStatement().addBatch();
                each.mapBatchIndex(batchIndex);
            }
            batchIndex++;
            getGeneratedKeyContext().addRow();
        } finally {
            resetBatch();
        }
    }
    
    private void resetBatch() throws SQLException {
        super.clearRouteContext();
        clearParameters();
    }
    
    @Override
    public int[] executeBatch() throws SQLException {
        try {
            return new PreparedStatementExecutor(getShardingConnection().getShardingContext().getExecutorEngine(), cachedPreparedStatementWrappers).executeBatch(batchIndex);
        } finally {
            clearRouteContext();
        }
    }
    
    private List<PreparedStatementExecutorWrapper> routeSQL() throws SQLException {
        List<PreparedStatementExecutorWrapper> result = new ArrayList<>();
        SQLRouteResult sqlRouteResult = preparedStatementRoutingEngine.route(getParameters());
        setSqlContext(sqlRouteResult.getSqlContext());
        if (sqlRouteResult.getSqlContext() instanceof InsertSQLContext) {
            setGeneratedKeyContext(((InsertSQLContext) sqlRouteResult.getSqlContext()).getGeneratedKeyContext());
        } else {
            setGeneratedKeyContext(new GeneratedKeyContext());
        }
        for (SQLExecutionUnit each : sqlRouteResult.getExecutionUnits()) {
            PreparedStatement preparedStatement = (PreparedStatement) getStatement(
                    getShardingConnection().getConnection(each.getDataSource(), sqlRouteResult.getSqlContext().getType()), each.getSQL());
            replayMethodsInvocation(preparedStatement);
            getParameters().replayMethodsInvocation(preparedStatement);
            result.add(wrap(preparedStatement, each));
        }
        return result;
    }
    
    private PreparedStatementExecutorWrapper wrap(final PreparedStatement preparedStatement, final SQLExecutionUnit sqlExecutionUnit) {
        Optional<PreparedStatementExecutorWrapper> wrapperOptional = Iterators.tryFind(cachedPreparedStatementWrappers.iterator(), new Predicate<PreparedStatementExecutorWrapper>() {
            @Override
            public boolean apply(final PreparedStatementExecutorWrapper input) {
                return Objects.equals(input.getPreparedStatement(), preparedStatement);
            }
        });
        if (wrapperOptional.isPresent()) {
            wrapperOptional.get().addBatchParameters(getParameters());
            return wrapperOptional.get();
        }
        PreparedStatementExecutorWrapper result = new PreparedStatementExecutorWrapper(preparedStatement, getParameters(), sqlExecutionUnit);
        cachedPreparedStatementWrappers.add(result);
        return result;
    }
    
    protected BackendStatementWrapper generateStatement(final Connection conn, final String shardingSql) throws SQLException {
        if (null != autoGeneratedKeys) {
            getGeneratedKeyContext().setAutoGeneratedKeys(autoGeneratedKeys);
            return new BackendPreparedStatementWrapper(conn.prepareStatement(shardingSql, autoGeneratedKeys), shardingSql);
        }
        if (null != columnIndexes) {
            getGeneratedKeyContext().setColumnIndexes(columnIndexes);
            return new BackendPreparedStatementWrapper(conn.prepareStatement(shardingSql, columnIndexes), shardingSql);
        }
        if (null != columnNames) {
            getGeneratedKeyContext().setColumnNames(columnNames);
            return new BackendPreparedStatementWrapper(conn.prepareStatement(shardingSql, columnNames), shardingSql);
        }
        if (0 != getResultSetHoldability()) {
            return new BackendPreparedStatementWrapper(conn.prepareStatement(shardingSql, getResultSetType(), getResultSetConcurrency(), getResultSetHoldability()), shardingSql);
        }
        return new BackendPreparedStatementWrapper(conn.prepareStatement(shardingSql, getResultSetType(), getResultSetConcurrency()), shardingSql);
    }
}
