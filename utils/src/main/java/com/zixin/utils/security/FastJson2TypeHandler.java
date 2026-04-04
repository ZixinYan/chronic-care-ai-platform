package com.zixin.utils.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@MappedTypes(JSON.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
@Component
public class FastJson2TypeHandler extends BaseTypeHandler<JSON> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSON parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setString(i, null);
            return;
        }
        ps.setString(i, parameter.toString());
    }

    @Override
    public JSON getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonStr = rs.getString(columnName);
        return parseJson(jsonStr);
    }

    @Override
    public JSON getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonStr = rs.getString(columnIndex);
        return parseJson(jsonStr);
    }

    @Override
    public JSON getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonStr = cs.getString(columnIndex);
        return parseJson(jsonStr);
    }

    private JSON parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(jsonStr, JSON.class, JSONReader.Feature.SupportAutoType);
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", jsonStr, e);
            try {
                return JSON.parseObject(jsonStr, JSON.class);
            } catch (Exception ex) {
                log.error("Failed to parse JSON as generic object: {}", jsonStr, ex);
                return null;
            }
        }
    }
}
