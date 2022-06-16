package com.timevale.drc.worker.service.sink.mysql;

import org.apache.commons.lang3.StringUtils;

/**
 * @author shanreng
 * mysql回环同步处理组件
 */
public class MysqlLoopSyncProcessUtil {

    /**
     * 基于UUID的DRC唯一标识
     * */
    private static final String UNIQUE_FLAG = "/* DRC-FLAG-c912e67f02394c4b86cca4c2b57baae5 */";

    public static String addDRCUniqueFlag(String originalSql){
        return UNIQUE_FLAG + originalSql;
    }

    public static StringBuilder addDRCUniqueFlag(StringBuilder originalSql){
        return originalSql.insert(0, UNIQUE_FLAG);
    }

    public static boolean isFromDRCSql(String sql){
        if(StringUtils.isEmpty(sql)){
            return false;
        }
        // startWith效率高一点，但是怕sql还会被其它中间件改写（后续可以优化为用正则匹配）
        return sql.startsWith(UNIQUE_FLAG);
    }
}
