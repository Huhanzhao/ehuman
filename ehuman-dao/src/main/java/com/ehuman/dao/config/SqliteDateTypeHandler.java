package com.ehuman.dao.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@MappedTypes(Date.class)
public class SqliteDateTypeHandler extends BaseTypeHandler<Date> {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
        // Java Date → SQLite TEXT
        ps.setString(i, DATE_FORMAT.format(parameter));
    }
    
    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String dateStr = rs.getString(columnName);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // SQLite TEXT → Java Date
            return DATE_FORMAT.parse(dateStr);
        } catch (Exception e) {
            // 如果存储的是时间戳数字，尝试转换
            try {
                long timestamp = Long.parseLong(dateStr);
                return new Date(timestamp);
            } catch (NumberFormatException ex) {
                throw new SQLException("Error parsing date: " + dateStr, e);
            }
        }
    }
    
    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String dateStr = rs.getString(columnIndex);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (Exception e) {
            try {
                long timestamp = Long.parseLong(dateStr);
                return new Date(timestamp);
            } catch (NumberFormatException ex) {
                throw new SQLException("Error parsing date: " + dateStr, e);
            }
        }
    }
    
    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String dateStr = cs.getString(columnIndex);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (Exception e) {
            try {
                long timestamp = Long.parseLong(dateStr);
                return new Date(timestamp);
            } catch (NumberFormatException ex) {
                throw new SQLException("Error parsing date: " + dateStr, e);
            }
        }
    }
}