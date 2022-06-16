package com.timevale.drc.pd.service.util;

import com.timevale.drc.base.util.JdbcTemplateManager;
import org.springframework.jdbc.core.RowMapper;

public class PkResolver {

    public <T> RowMapper<T> resolver(JdbcTemplateManager.FieldType fieldType) {
        if (fieldType == JdbcTemplateManager.FieldType.BIGINT) {
            return (RowMapper<T>) new BigIntFieldResolver();
        }
        if (fieldType == JdbcTemplateManager.FieldType.INT) {
            return (RowMapper<T>) new IntFieldResolver();
        } else {
            return (RowMapper<T>) new StringFieldResolver();
        }
    }
}
