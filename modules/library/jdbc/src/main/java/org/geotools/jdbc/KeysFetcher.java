/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.jdbc;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * In charge for setting/getting the primary key values for inserts.
 */
abstract class KeysFetcher {
    protected final PrimaryKey key;
    private final Set<String> columnNames;

    /**
     * Token for making the difference between key values that are not set before insert and NULL
     * keys (GeoPkgDialect#getNextAutoGeneratedValue always return NULL and sqlite autogenerates
     * if the PK column is NULL).
     */
    protected static Object NOT_SET_BEFORE_INSERT = new Object();


    protected KeysFetcher(PrimaryKey key) {
        this.key = key;
        columnNames = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(getColumnNames())));
    }

    public static KeysFetcher create(JDBCDataStore ds, Connection cx, boolean useExisting,
                                     PrimaryKey key)
            throws SQLException, IOException {
        if (useExisting) {
            return new Existing(ds.getSQLDialect(), key);
        } else {
            return new FromDB(ds, cx, key);
        }
    }

    /**
     * Set all the key values (the ones that are known before insert) for a prepared statement.
     */
    public int setKeyValues(PreparedStatementSQLDialect dialect, PreparedStatement ps,
                            Connection cx, SimpleFeatureType featureType, SimpleFeature feature,
                            int curFieldPos)
            throws IOException, SQLException {
        final List<Object> keyValues = getNextValues(cx, feature);
        for (int i = 0; i < key.getColumns().size(); i++) {
            final PrimaryKeyColumn col = key.getColumns().get(i);
            final Object value = keyValues.get(i);
            if (value != NOT_SET_BEFORE_INSERT) {
                dialect.setValue(value, col.getType(), ps, curFieldPos++, cx);
            }
        }

        if (!isPostInsert()) {
            //report the feature id as user data since we cant set the fid.
            String fid = featureType.getTypeName() + "." + JDBCDataStore.encodeFID(keyValues);
            feature.getUserData().put("fid", fid);
        }

        return curFieldPos;
    }

    /**
     * Set all the key values  (the ones that are known before insert) for a non-prepared statement.
     */
    public void setKeyValues(JDBCDataStore ds, Connection cx, SimpleFeatureType featureType,
                             SimpleFeature feature, StringBuffer sql)
            throws IOException, SQLException {
        BasicSQLDialect dialect = (BasicSQLDialect) ds.getSQLDialect();
        List<Object> keyValues = getNextValues(cx, feature);
        for (int i = 0; i < key.getColumns().size(); i++) {
            PrimaryKeyColumn col = key.getColumns().get(i);
            Object value = keyValues.get(i);
            if (value != NOT_SET_BEFORE_INSERT) {
                try {
                    dialect.encodeValue(value, col.getType(), sql);
                    sql.append(",");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!isPostInsert()) {
            //report the feature id as user data since we cant set the fid. postInsert may overwrite it.
            String fid = featureType.getTypeName() + "." + JDBCDataStore.encodeFID(keyValues);
            feature.getUserData().put("fid", fid);
        }
    }

    public abstract void addKeyColumns(StringBuffer sql);

    public abstract void addKeyBindings(StringBuffer sql);

    /**
     * Called after a batch prepared statement insert to get back the keys that were inserted.
     */
    public abstract void postInsert(SimpleFeatureType featureType,
                                    Collection<SimpleFeature> features,
                                    PreparedStatement ps) throws SQLException;

    /**
     * Called after each non-prepared statement inserts to get back the key that were inserted.
     * @deprecated Please call {@link #postInsert(SimpleFeatureType, SimpleFeature, Connection, Statement)} instead
     */
    @Deprecated
    public void postInsert(SimpleFeatureType featureType, SimpleFeature feature,
                                    Connection cx) throws SQLException {
        postInsert(featureType, feature, cx, null);
    }
    
    /**
     * Called after each non-prepared statement inserts to get back the key that were inserted.
     */
    public abstract void postInsert(SimpleFeatureType featureType, SimpleFeature feature,
                                    Connection cx, Statement st) throws SQLException;


    /**
     * @return true if some key values must be fetched after insert.
     */
    public abstract boolean isPostInsert();
    
    /**
     * @return true if some key value is auto generated by the database and we need
     * to execute the statement passing the Statement.RETURN_GENERATED_KEYS flag 
     */
    public boolean hasAutoGeneratedKeys() {
        return false;
    }

    protected abstract List<Object> getNextValues(Connection cx, SimpleFeature feature)
            throws IOException, SQLException;

    /**
     * @return true if the given field is part of the primary key.
     */
    public boolean isKey(String name) {
        return columnNames.contains(name);
    }

    /**
     * @return the key column names in the order expected in the {@linkplain ResultSet} returned by
     * {@linkplain PreparedStatement#getGeneratedKeys()}.
     */
    public String[] getColumnNames() {
        String[] ret = new String[key.getColumns().size()];
        int i = 0;
        for (PrimaryKeyColumn col: key.getColumns()) {
            ret[i++] = col.getName();
        }
        return ret;
    }

    /**
     * When the PK is set by the user in the feature ID.
     */
    private static class Existing extends KeysFetcher {
        private final String keyColumnNames;

        public Existing(SQLDialect dialect, PrimaryKey key) {
            super(key);
            final StringBuffer keyColumnNames = new StringBuffer();
            for (PrimaryKeyColumn col : key.getColumns()) {
                dialect.encodeColumnName(null, col.getName(), keyColumnNames);
                keyColumnNames.append(",");
            }
            this.keyColumnNames = keyColumnNames.toString();
        }

        @Override
        public void addKeyColumns(StringBuffer sql) {
            sql.append(keyColumnNames);
        }

        @Override
        public void addKeyBindings(StringBuffer sql) {
            for (int i = 0; i < key.getColumns().size(); ++i) {
                sql.append("?,");
            }
        }

        @Override
        public void postInsert(SimpleFeatureType featureType, Collection<SimpleFeature> features,
                               PreparedStatement ps) {
        }

        @Override
        public void postInsert(SimpleFeatureType featureType, SimpleFeature feature, Connection cx, Statement st)
                throws SQLException {
        }

        @Override
        public boolean isPostInsert() {
            return false;
        }

        @Override
        public List<Object> getNextValues(Connection cx, SimpleFeature feature) {
            return JDBCDataStore.decodeFID(key, feature.getID(), true);
        }

    }

    /**
     * Class for a PK that has it's value computed from the database.
     */
    private static class FromDB extends KeysFetcher {
        private final List<KeyFetcher> fetchers;

        public FromDB(JDBCDataStore ds, Connection cx, PrimaryKey key)
                throws SQLException, IOException {
            super(key);
            fetchers = new ArrayList<>(key.getColumns().size());
            for (PrimaryKeyColumn col : key.getColumns()) {
                fetchers.add(createKeyFetcher(ds, cx, key, col));
            }
        }

        private KeyFetcher createKeyFetcher(JDBCDataStore ds, Connection cx, PrimaryKey key,
                                            PrimaryKeyColumn col)
                throws SQLException, IOException {
            final Class t = col.getType();
            if (col instanceof AutoGeneratedPrimaryKeyColumn) {
                return new AutoGenerated(ds, key, col);
            } else if (col instanceof SequencedPrimaryKeyColumn) {
                return new FromSequence(ds, col);
            } else {
                //try to calculate

                //is the column numeric?
                if (Number.class.isAssignableFrom(t)) {
                    //is the column integral?
                    if (t == Short.class || t == Integer.class || t == Long.class
                            || BigInteger.class.isAssignableFrom(t)
                            || BigDecimal.class.isAssignableFrom(t)) {
                        return new FromPreviousIntegral(ds, cx, key, col);
                    }
                } else if (CharSequence.class.isAssignableFrom(t)) {
                    return new FromRandom(ds, col);
                }
            }
            throw new IOException("Cannot generate key value for column of type: " + t.getName());
        }

        @Override
        public void addKeyColumns(StringBuffer sql) {
            for (KeyFetcher fetcher : fetchers) {
                fetcher.addKeyColumn(sql);
            }
        }

        @Override
        public void addKeyBindings(StringBuffer sql) {
            for (KeyFetcher fetcher : fetchers) {
                fetcher.addKeyBinding(sql);
            }
        }

        @Override
        public void postInsert(SimpleFeatureType featureType, Collection<SimpleFeature> features,
                               PreparedStatement ps) throws SQLException {
            if (!isPostInsert()) {
                return;
            }
            final ResultSet rs = ps.getGeneratedKeys();
            try {
                final Iterator<SimpleFeature> it = features.iterator();
                final List<Object> keyValues = new ArrayList<>(key.getColumns().size());
                while (rs.next()) {
                    final SimpleFeature feature = it.next();
                    // Need to access the values by index instead of name because of a limitation in
                    // Oracle. It is assumed the result set contains only the keys and in the
                    // correct order since they where declared like that when the PreparedStatement
                    // was created.
                    for (int index = 1; index <= key.getColumns().size(); ++index) {
                        keyValues.add(rs.getObject(index));
                    }
                    String fid = featureType.getTypeName() + "." +
                            JDBCDataStore.encodeFID(keyValues);
                    feature.getUserData().put("fid", fid);
                    keyValues.clear();
                }
            } finally {
                rs.close();
            }
        }
        
        @Override
        public void postInsert(SimpleFeatureType featureType, SimpleFeature feature, Connection cx, Statement st)
                throws SQLException {
            if (!isPostInsert()) {
                return;
            }
            List<Object> keyValues = getLastValues(cx, st);
            String fid = featureType.getTypeName() + "." + JDBCDataStore.encodeFID(keyValues);
            feature.getUserData().put("fid", fid);
        }

        @Override
        public boolean isPostInsert() {
            for (KeyFetcher fetcher : fetchers) {
                if (fetcher.isPostInsert()) {
                    return true;
                }
            }
            return false;
        }

        private List<Object> getLastValues(Connection cx, Statement st) throws SQLException {
            List<Object> last = new ArrayList<>();
            for (KeyFetcher fetcher : fetchers) {
                last.add(fetcher.getLastValue(cx, st));
            }
            return last;
        }

        @Override
        public List<Object> getNextValues(Connection cx, SimpleFeature feature)
                throws IOException, SQLException {
            List<Object> ret = new ArrayList<>(fetchers.size());
            for (KeyFetcher fetcher : fetchers) {
                ret.add(fetcher.getNext(cx));
            }
            return ret;
        }

        @Override
        public boolean hasAutoGeneratedKeys() {
            for (KeyFetcher fetcher : fetchers) {
                if(fetcher.isAutoGenerated()) {
                    return true;
                }
            }
            return false;
        }
        
        
    }

    /**
     * Base class to handle a PK column comming from the DB.
     */
    private static abstract class KeyFetcher {
        private final String colName;
        protected final PrimaryKeyColumn col;

        public abstract Object getNext(Connection cx) throws IOException, SQLException;

        KeyFetcher(JDBCDataStore ds, PrimaryKeyColumn col) {
            this.col = col;
            StringBuffer colName = new StringBuffer();
            ds.getSQLDialect().encodeColumnName(null, col.getName(), colName);
            this.colName = colName.toString();
        }

        public void addKeyColumn(StringBuffer sql) {
            sql.append(colName);
            sql.append(",");
        }

        public void addKeyBinding(StringBuffer sql) {
            sql.append("?,");
        }

        public abstract Object getLastValue(Connection cx, Statement st) throws SQLException;
        
        /**
         * Returns the last generated value based on the connection. Deprecated, please use/implement the
         * version taking also the statement as an argument
         * @param cx
         * @return
         * @throws SQLException
         */
        public Object getLastValue(Connection cx) throws SQLException {
            return getLastValue(cx, null);
        }

        public abstract boolean isPostInsert();
        
        public boolean isAutoGenerated() {
            return false;
        }

    }

    private static class FromRandom extends KeyFetcher {
        FromRandom(JDBCDataStore ds, PrimaryKeyColumn col) {
            super(ds, col);
        }

        @Override
        public Object getLastValue(Connection cx, Statement st) {
            throw new IllegalArgumentException("Column " + col.getName() + " is not generated.");
        }

        @Override
        public boolean isPostInsert() {
            return false;
        }

        @Override
        public Object getNext(Connection cx) {
            return SimpleFeatureBuilder.createDefaultFeatureId();
        }
    }

    /**
     * For PK columns that have no sequence at all. Take the max()+1 from the existing features
     * and use that value.
     */
    protected static class FromPreviousIntegral extends KeyFetcher {
        private Object next;

        public FromPreviousIntegral(JDBCDataStore ds, Connection cx, PrimaryKey key,
                                    PrimaryKeyColumn col) throws SQLException {
            super(ds, col);
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT MAX(");
            ds.getSQLDialect().encodeColumnName(null, col.getName(), sql);
            sql.append(") + 1 FROM ");
            ds.encodeTableName(key.getTableName(), sql, null);

            Statement st = cx.createStatement();
            try {
                ResultSet rs = st.executeQuery(sql.toString());
                try {
                    rs.next();
                    next = rs.getObject(1);

                    if (next == null) {
                        //this probably means there was no data in the table, set to 1
                        //TODO: probably better to do a count to check... but if this
                        // value already exists the db will throw an error when it tries
                        // to insert
                        next = 1;
                    }
                } finally {
                    rs.close();
                }
            } finally {
                st.close();
            }

        }

        @Override
        public Object getNext(Connection cx) throws IOException {
            Object result = next;
            next = increment(next);
            return result;
        }

        @Override
        public Object getLastValue(Connection cx, Statement st) {
            throw new IllegalArgumentException("Column " + col.getName() + " is not generated.");
        }

        @Override
        public boolean isPostInsert() {
            return false;
        }

        public static Object increment(Object value) throws IOException {
            if (value instanceof Integer) {
                return ((Integer) value) + 1;
            } else if (value instanceof Long) {
                return ((Long) value) + 1;
            } else if (value instanceof Short) {
                return (short) (((Short) value) + 1);
            } else if (value instanceof BigDecimal) {
                return ((BigDecimal) value).add(BigDecimal.ONE);
            } else if (value instanceof BigInteger) {
                return ((BigInteger) value).add(BigInteger.ONE);
            } else {
                throw new IOException("Don't know how to increment a number of class " +
                        value.getClass().getSimpleName());
            }
        }
    }

    private static class AutoGenerated extends KeyFetcher {
        private final JDBCDataStore ds;
        private final PrimaryKey key;

        public AutoGenerated(JDBCDataStore ds, PrimaryKey key, PrimaryKeyColumn col) {
            super(ds, col);
            this.ds = ds;
            this.key = key;
        }

        @Override
        public Object getNext(Connection cx) throws IOException, SQLException {
            if (isPostInsert()) {
                return NOT_SET_BEFORE_INSERT;
            } else {
                return ds.getSQLDialect().getNextAutoGeneratedValue(ds.getDatabaseSchema(),
                        key.getTableName(), col.getName(), cx);
            }
        }

        @Override
        public void addKeyColumn(StringBuffer sql) {
            if (!isPostInsert()) {
                super.addKeyColumn(sql);
            }
        }

        @Override
        public void addKeyBinding(StringBuffer sql) {
            if (!isPostInsert()) {
                super.addKeyBinding(sql);
            }
        }

        @Override
        public Object getLastValue(Connection cx, Statement st) throws SQLException {
            return ds.getSQLDialect().getLastAutoGeneratedValue(ds.getDatabaseSchema(),
                    key.getTableName(), col.getName(), cx, st);
        }

        @Override
        public boolean isPostInsert() {
            return ds.getSQLDialect().lookupGeneratedValuesPostInsert();
        }
        
        @Override
        public boolean isAutoGenerated() {
            // we'll get the Statement.RETURN_GENERATED_KEYS flag added only if it's going to be inspected
            return isPostInsert();
        }
    }

    private static class FromSequence extends KeyFetcher {
        private final JDBCDataStore ds;

        public FromSequence(JDBCDataStore ds, PrimaryKeyColumn col) {
            super(ds, col);
            this.ds = ds;
        }

        @Override
        public void addKeyBinding(StringBuffer sql) {
            if (isPostInsert()) {
                String sequenceName = ((SequencedPrimaryKeyColumn) col).getSequenceName();
                sql.append(ds.getSQLDialect().encodeNextSequenceValue(null, sequenceName));
                sql.append(",");
            } else {
                super.addKeyBinding(sql);
            }
        }

        @Override
        public Object getLastValue(Connection cx, Statement st) throws SQLException {
            throw new IllegalArgumentException("Column " + col.getName() + " is not generated.");
        }

        @Override
        public boolean isPostInsert() {
            return ds.getSQLDialect().lookupGeneratedValuesPostInsert() &&
                    ds.getSQLDialect() instanceof PreparedStatementSQLDialect;
        }

        @Override
        public Object getNext(Connection cx) throws IOException, SQLException {
            if(isPostInsert()) {
                return NOT_SET_BEFORE_INSERT;
            } else {
                String sequenceName = ((SequencedPrimaryKeyColumn) col).getSequenceName();
                return ds.getSQLDialect().getNextSequenceValue(ds.getDatabaseSchema(), sequenceName,
                        cx);
            }
        }
    }

}
