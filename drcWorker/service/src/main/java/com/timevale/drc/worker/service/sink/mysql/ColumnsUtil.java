//package com.timevale.drc.worker.service.sink.mysql;
//
//import com.timevale.drc.base.util.SQL;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author gwk_2
// * @date 2021/9/30 14:42
// */
//public class ColumnsUtil {
//
//    static Map<String, List<String>> cache = new HashMap<>();
//
//    public static List<String> getColumns(String tableName, JdbcTemplate jdbcTemplate) {
//        if (cache.get(tableName) != null) {
//            return cache.get(tableName);
//        }
//        String getAllFieldMetaDataSql = SQL.getAllFieldMetaData(tableName);
//        List<String> list = jdbcTemplate.queryForObject(getAllFieldMetaDataSql, (rs, rowNum) -> {
//            List<String> set = new ArrayList<>();
//            set.add(rs.getString(1));
//            while (rs.next()) {
//                String string = rs.getString(1);
//                set.add(string);
//            }
//            return set;
//        });
//
//        cache.put(tableName, list);
//        return list;
//    }
//
//    public static String getColumnsString(String tableName, JdbcTemplate jdbcTemplate) {
//        StringBuilder sb = new StringBuilder();
//        for (String key : getColumns(tableName, jdbcTemplate)) {
//            sb.append(key).append(",");
//        }
//        return sb.substring(0, sb.length() - 1);
//    }
//
//    static class Key {
//
//    }
//}
