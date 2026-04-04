package com.zixin.utils.security;

import org.apache.ibatis.type.*;
import org.springframework.stereotype.Component;

import java.sql.*;

@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
@Component
public class SensitiveDataEncryptHandler extends BaseTypeHandler<String> {

    private final AesProperties aesProperties;

    public SensitiveDataEncryptHandler(AesProperties aesProperties) {
        this.aesProperties = aesProperties;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            CryptoService crypto = new CryptoService(
                    aesProperties.getKeys(),
                    aesProperties.getCurrent()
            );
            ps.setString(i, crypto.encrypt(parameter));
        } catch (Exception e) {
            throw new SQLException("Encryption failed", e);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String val) throws SQLException {
        try {
            CryptoService crypto = new CryptoService(
                    aesProperties.getKeys(),
                    aesProperties.getCurrent()
            );
            return crypto.decrypt(val);
        } catch (Exception e) {
            throw new SQLException("Decrypt failed", e);
        }
    }
}