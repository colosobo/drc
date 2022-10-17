package com.timevale.drc.worker.service.sink.mysql;

import com.timevale.drc.base.model.DrcDbConfig;
import com.timevale.drc.base.mysql.MigrateTableStructureUtil;
import com.timevale.drc.base.util.JdbcTemplateManager;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class MigrateTableStructureUtilTest {

    @Test
    public void migrateTableStructure() {

        MigrateTableStructureUtil migrateTableStructureUtil = new MigrateTableStructureUtil();
        DrcDbConfig old = new DrcDbConfig("192.168.2.2:3306", "root", "null", "cr");
        ;
        DrcDbConfig newC = new DrcDbConfig("192.168.2.2:3306", "root", "null", "xm");
        String table = "sand_test";
        migrateTableStructureUtil.migrateTableStructure(old, newC, table, table);


        Set<String> o = new JdbcTemplateManager().get("192.168.2.2:3306", "root", "null", "cr").
                queryForObject("select COLUMN_NAME from information_schema.COLUMNS where table_name = 'sand_test';", new RowMapper<Set<String>>() {
                    @Override
                    public Set<String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Set<String> set = new HashSet<>();
                        while (rs.next()) {
                            String string = rs.getString(1);
                            set.add(string);
                        }
                        return set;
                    }
                });
        System.out.println(o);
    }

    @Test
    public void f() {
        JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();
        Set<String> strings = jdbcTemplateManager.showTables("192.168.2.2:3306", "root", "null", "cr");
        System.out.println(strings);
    }
}
