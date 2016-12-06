package ulcambridge.foundations.viewer.crowdsourcing.dao;


import io.jsonwebtoken.lang.Assert;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A view of an SqlRowSet which prevents users changing the current row, and can
 * be made invalid to prevent unintended access to a different row if the
 * target SqlRowSet the view delegates to is repositioned externally.
 *
 * <p>Client code is not intended to know/care it's using this particular
 * implementation of {@link SqlRowSet}, in the same way that client code needn't
 * know it's using a wrapper from a method like
 * {@link Collections#unmodifiableList(List)}.
 *
 * <p>Code creating instances of this class is responsible for calling
 * {@link #invalidate()} if the target {@link SqlRowSet} is repositioned. If
 * this is not done then client code could find itself unknowingly referencing a
 * different row.
 */
public class SingleRowSqlRowSetView implements SqlRowSet {
    private SqlRowSet target;

    /**
     * Create a read-only view of the target row set.
     */
    public SingleRowSqlRowSetView(SqlRowSet target) {
        Assert.notNull(target);

        this.target = target;
    }

    public boolean isValid() {
        return target != null;
    }

    public void invalidate() {
        this.target = null;
    }

    private SqlRowSet getTarget() {
        if(target == null)
            throw new IllegalStateException(
                "Attempted to access view of SqlRowSet after the active row " +
                    "changed");

        return target;
    }

    @Override
    public boolean isAfterLast() throws InvalidResultSetAccessException {
        return getTarget().isAfterLast();
    }

    @Override
    public SqlRowSetMetaData getMetaData() {
        return getTarget().getMetaData();
    }

    @Override
    public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().findColumn(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getBigDecimal(columnLabel);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getBoolean(columnIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getBoolean(columnLabel);
    }

    @Override
    public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getByte(columnIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getByte(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getDate(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getDate(columnLabel, cal);
    }

    @Override
    public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getDouble(columnIndex);
    }

    @Override
    public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getDouble(columnLabel);
    }

    @Override
    public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getFloat(columnIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getFloat(columnLabel);
    }

    @Override
    public int getInt(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getInt(columnIndex);
    }

    @Override
    public int getInt(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getInt(columnLabel);
    }

    @Override
    public long getLong(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getLong(columnIndex);
    }

    @Override
    public long getLong(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getLong(columnLabel);
    }

    @Override
    public String getNString(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getNString(columnLabel);
    }

    @Override
    public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnLabel);
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnIndex, map);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnLabel, map);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
        return getTarget().getObject(columnLabel, type);
    }

    @Override
    public short getShort(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getShort(columnIndex);
    }

    @Override
    public short getShort(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getShort(columnLabel);
    }

    @Override
    public String getString(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getString(columnIndex);
    }

    @Override
    public String getString(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getString(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getTime(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getTime(columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
        return getTarget().getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
        return getTarget().getTimestamp(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return getTarget().getTimestamp(columnLabel, cal);
    }

    @Override
    public int getRow() throws InvalidResultSetAccessException {
        return getTarget().getRow();
    }

    @Override
    public boolean isBeforeFirst() throws InvalidResultSetAccessException {
        return getTarget().isBeforeFirst();
    }

    @Override
    public boolean isFirst() throws InvalidResultSetAccessException {
        return getTarget().isFirst();
    }

    @Override
    public boolean isLast() throws InvalidResultSetAccessException {
        return getTarget().isLast();
    }

    @Override
    public boolean wasNull() throws InvalidResultSetAccessException {
        return getTarget().wasNull();
    }

    private RuntimeException unsupportedMutationMethod() {
        return new UnsupportedOperationException(
            "Calling methods which modify the state of this " +
                this.getClass().getSimpleName() + " is not permitted.");
    }

    @Override
    public boolean absolute(int row) throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public void afterLast() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public void beforeFirst() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public boolean first() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public boolean last() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public boolean next() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public boolean previous() throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }

    @Override
    public boolean relative(int rows) throws InvalidResultSetAccessException {
        throw unsupportedMutationMethod();
    }
}
