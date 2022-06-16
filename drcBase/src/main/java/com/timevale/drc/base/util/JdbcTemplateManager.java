package com.timevale.drc.base.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.collect.Sets;
import com.timevale.drc.base.mysql.MySqlKeyUtil;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author gwk_2
 * @date 2021/3/4 15:30
 */
@Slf4j
public class JdbcTemplateManager {

    public static final String JDBC_PREFIX = "jdbc:mysql://";

    private static Map<String, JdbcTemplate> cache = new ConcurrentHashMap<>();

    public JdbcTemplate get(String url, String username, String pwd, String database){
        // 默认不支持回环同步
        return get(url,username,pwd,database,false);
    }

    /**
     * 获取spring jdbc模板.
     *
     * @param url
     * @param username
     * @param pwd
     * @param database
     * @return
     */
    public JdbcTemplate get(String url, String username, String pwd, String database, boolean supportLoopSync) {
        String key = url + username + pwd + database + GlobalConfigUtil.jdbcParams();
        if (cache.get(key) != null) {
            return cache.get(key);
        }
        try {
            if (!url.startsWith(JDBC_PREFIX)) {
                String p = GlobalConfigUtil.jdbcParams();
                if (StringUtils.isBlank(p)) {
                    url = JDBC_PREFIX + url + "/" + database + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&autoReconnect=true&createDatabaseIfNotExist=true&tinyInt1isBit=false";
                } else {
                    url = JDBC_PREFIX + url + "/" + database + "?" + p;
                }
            }

            if(supportLoopSync) {
                // 支持回环同步，需要开启session级别的binlog_rows_query_log_events
                url = url + "&sessionVariables=binlog_rows_query_log_events=1";
            }

            synchronized (this) {
                CheckDataSourceValidResult valid = checkDataSourceValid(url, username, pwd, database);
                if (!valid.isValid()) {
                    throw new RuntimeException("创建 JdbcTemplate 失败, 原因:" + valid.getMsg());
                }
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setUrl(url);
                dataSource.setUsername(username);
                dataSource.setPassword(pwd);
                dataSource.setMaxActive(GlobalConfigUtil.jdbcConnectionMaxPool());
                dataSource.init();
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                cache.put(key, jdbcTemplate);
                return jdbcTemplate;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取所有表名.
     *
     * @param url
     * @param username
     * @param pwd
     * @param database
     * @return
     */
    public Set<String> showTables(String url, String username, String pwd, String database) {
        JdbcTemplate jdbcTemplate = get(url, username, pwd, database);

        List<Map<String, Object>> list = jdbcTemplate.queryForList("show TABLES");
        Set<String> set = Sets.newHashSet();
        list.forEach(i -> set.addAll(i.values().stream().map(Object::toString).collect(Collectors.toList())));
        return set;
    }

    /**
     * 获取字段类型
     *
     * @param jdbcTemplate
     * @param tableName
     * @param fieldName
     * @return
     */
    public FieldType getFieldType(JdbcTemplate jdbcTemplate, String tableName, String fieldName) {
        String sql = SQL.getAllFieldMetaData(tableName);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> item : list) {
            String o = (String) item.get("Field");
            if (fieldName.equals(o)) {
                String type = (String) item.get("Type");
                for (FieldType value : FieldType.values()) {
                    if (type.contains("(")) {
                        if (type.substring(0, type.indexOf("(")).trim().toUpperCase().equals(value.name())) {
                            return value;
                        }
                    } else {
                        if (type.trim().toUpperCase().equals(value.name())) {
                            return value;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取 db 字段的类型.
     */
    public FieldType getFieldType(String url, String username, String password, String database, String tableName, String fieldName) {
        JdbcTemplate jdbcTemplate = get(url, username, password, database);
        return getFieldType(jdbcTemplate, tableName, fieldName);
    }

    /**
     * 检查 db 配置.
     *
     * @param url
     * @param username
     * @param password
     * @param database
     * @param tableName
     */
    public void checkDbConfig(String url, String username, String password, String database, String tableName) {
        CheckDataSourceValidResult result = checkDataSourceValid(url, username, password, database);
        if (!result.isValid()) {
            throw new RuntimeException(result.getMsg());
        }
        CheckTableExistResult tableExistResult = checkTableExists(url, username, password, database, tableName);
        // 表不存在.
        if (!tableExistResult.isExists()) {
            throw new RuntimeException(tableExistResult.getMsg());
        }
        // 主键是否存在
        checkPkExists(url, username, password, database, tableName);
    }

    /**
     * 检查指定的表里, 有没有主键. 当然, 也可能是多个主键,但是只取第一个.
     */
    public void checkPkExists(String url, String username, String password, String database, String tableName) {
        if (MySqlKeyUtil.exists(tableName)) {
            tableName = MySqlKeyUtil.conv(tableName);
        }
        String pkFromTable = getPkFromTable(url, username, password, database, tableName);
        if (StringUtils.isBlank(pkFromTable)) {
            throw new RuntimeException(tableName + "没找到主键.");
        }
    }

    /**
     * 获取主键.
     *
     * @param url
     * @param username
     * @param password
     * @param database
     * @param tableName
     * @return
     */
    public String getPkFromTable(String url, String username, String password, String database, String tableName) {
        if (MySqlKeyUtil.exists(tableName)) {
            tableName = MySqlKeyUtil.conv(tableName);
        }
        String sql = "show create table " + tableName;
        Connection connection = null;
        try {
            connection = getConnection(url, username, password, database);
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            while (resultSet.next()) {
                String result = resultSet.getString(2);
                //  以一种很 low 的方式获取 主键, 只为了兼容 DRDS.
                int primary_key = result.indexOf("PRIMARY KEY");
                int i = result.indexOf("(", primary_key);
                int i1 = result.indexOf("`", i);
                int i2 = result.indexOf("`", i1 + 1);
                return result.substring(i1 + 1, i2);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    /**
     * 检查表是否存在.
     */
    public CheckTableExistResult checkTableExists(String url, String username, String password, String database, String tableName) {
//        if (MySqlKey.exists(tableName)) {
//            tableName = MySqlKey.conv(tableName);// 貌似不需要转义.
//        }
        String sql = SQL.getTable(database, tableName);
        Connection connection = null;
        try {
            connection = getConnection(url, username, password, database);
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            while (resultSet.next()) {
                String result = resultSet.getString(1);
                if (result != null && result.equals(tableName)) {
                    return CheckTableExistResult.buildSuccess();
                }
            }
        } catch (Exception e) {
            return CheckTableExistResult.buildError(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return CheckTableExistResult.buildError("表不存在");
    }


    /**
     * 检查数据是否有效.
     */
    public CheckDataSourceValidResult checkDataSourceValid(String url, String username, String password, String dataBase) {
        String sql = SQL.select1();
        Connection connection = null;
        try {
            connection = getConnection(url, username, password, dataBase);
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            while (resultSet.next()) {
                int result = resultSet.getInt(1);
                if (result == 1) {
                    return CheckDataSourceValidResult.buildSuccess();
                }
            }
        } catch (Exception e) {
            return CheckDataSourceValidResult.buildError(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return CheckDataSourceValidResult.buildError("数据源错误.");
    }

    private Connection getConnection(String url, String username, String password, String database) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        if (!url.startsWith(JDBC_PREFIX)) {
            url = JDBC_PREFIX + url + "/" + database;
        }
        return DriverManager.getConnection(url, username, password);
    }

    @Getter
    public enum FieldType {
        TINYINT(Integer.class),
        BIGINT(Integer.class),
        INT(Integer.class),
        SMALLINT(Integer.class),
        MEDIUMINT(Integer.class),
        INTEGER(Integer.class),
        FLOAT(Integer.class),
        DOUBLE(Integer.class),
        DECIMAL(Integer.class),

        CHAR(String.class),// 定长字符串
        TINYBLOB(String.class),// 不超过 255 个字符的二进制字符串
        TINYTEXT(String.class), // 短文本字符串
        BLOB(String.class), // 二进制形式的长文本数据
        MEDIUMBLOB(String.class), // 二进制形式的中等长度文本数据
        MEDIUMTEXT(String.class),// 中等长度文本数据
        LONGBLOB(String.class),// 二进制形式的极大文本数据
        LONGTEXT(String.class),//极大文本数据
        VARCHAR(String.class), // 变长字符串
        TEXT(String.class), // 长文本数据


        DATETIME(String.class),// YYYY-MM-DD HH:MM:SS
        TIMESTAMP(String.class), // YYYYMMDD HHMMSS
        DATE(String.class),// YYYY-MM-DD
        TIME(String.class),// HH:MM:SS
        YEAR(String.class), // YYYY

        BIT(Integer.class);// m的范围在1到64之间


        Class<?> aClass;

        public Class<?> findClass() {
            return aClass;
        }

        FieldType(Class<?> aClass) {
            this.aClass = aClass;
        }
    }
}
