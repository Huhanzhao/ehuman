package com.bot.service.common.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@MappedTypes(LocalDateTime.class)
public class SqliteDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {
    
    // 强制使用空格分隔，不要用 T
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        // 写入时：LocalDateTime → 空格格式的字符串
        ps.setString(i, parameter.format(FORMATTER));
    }
    
    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String dateStr = rs.getString(columnName);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // 读取时：字符串 → LocalDateTime
        return LocalDateTime.parse(dateStr, FORMATTER);
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String dateStr = rs.getString(columnIndex);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // 如果是时间戳数字，尝试转换
            if (dateStr.matches("\\d+")) {
                long timestamp = Long.parseLong(dateStr);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            }
            // 去掉可能存在的 T 和毫秒部分，统一格式
            dateStr = dateStr.replace('T', ' ');
            if (dateStr.contains(".")) {
                dateStr = dateStr.substring(0, dateStr.indexOf("."));
            }
            return LocalDateTime.parse(dateStr, FORMATTER);
        } catch (Exception e) {
            System.err.println("Error parsing date at columnIndex " + columnIndex + ": " + dateStr);
            return null;
        }
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String dateStr = cs.getString(columnIndex);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // 如果是时间戳数字，尝试转换
            if (dateStr.matches("\\d+")) {
                long timestamp = Long.parseLong(dateStr);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            }
            // 去掉可能存在的 T 和毫秒部分，统一格式
            dateStr = dateStr.replace('T', ' ');
            if (dateStr.contains(".")) {
                dateStr = dateStr.substring(0, dateStr.indexOf("."));
            }
            return LocalDateTime.parse(dateStr, FORMATTER);
        } catch (Exception e) {
            System.err.println("Error parsing date from CallableStatement at index " + columnIndex + ": " + dateStr);
            return null;
        }
    }
}