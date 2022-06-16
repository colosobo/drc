package com.timevale.drc.base.mysql;

import com.timevale.drc.base.model.DrcDbConfig;
import com.timevale.drc.base.util.CheckTableExistResult;
import com.timevale.drc.base.util.JdbcTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author gwk_2
 * @date 2021/4/22 11:42
 */
@Slf4j
public class MigrateTableStructureUtil {

    private static JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();

    public static boolean migrateTableStructure(DrcDbConfig old, DrcDbConfig newC, String originTableName, String sinkTableName) {

        if (StringUtils.isBlank(originTableName)) {
            throw new RuntimeException("tableName 不能是空.");
        }

        if (StringUtils.isBlank(sinkTableName)) {
            throw new RuntimeException("sinkTableName 不能是空");
        }

        originTableName = originTableName.trim();
        sinkTableName = sinkTableName.trim();

        CheckTableExistResult checkTableExists = jdbcTemplateManager.checkTableExists(newC.getUrl(), newC.getUsername(), newC.getPassword(), newC.getDatabaseName(), sinkTableName);
        // true 表存在.
        if (checkTableExists.isExists()) {
            log.warn("表存在, 不用迁移,msg={}", checkTableExists.getMsg());
            return false;
        }

        JdbcTemplate oldJdbcTemplate = jdbcTemplateManager.get(old.getUrl(), old.getUsername(), old.getPassword(), old.getDatabaseName());
        JdbcTemplate newJdbcTemplate = jdbcTemplateManager.get(newC.getUrl(), newC.getUsername(), newC.getPassword(), newC.getDatabaseName());

        String sql = "SHOW CREATE TABLE " + originTableName;
        // 获取老库中, 创建 SQL 的语句.
        String tableSQL = oldJdbcTemplate.queryForObject(sql, new GetTableSQLRowMapper());
        if (tableSQL == null) {
            throw new RuntimeException("找不 CREATE TABLE 语句.");
        }
        // 替换表名.
        String newSql = tableSQL.replaceFirst(originTableName, sinkTableName);
        // 在新库中, 创建新的表.
        try {
            newJdbcTemplate.execute(newSql);
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                // ignore
            } else {
                throw e;
            }
        }
        return true;
    }
}
