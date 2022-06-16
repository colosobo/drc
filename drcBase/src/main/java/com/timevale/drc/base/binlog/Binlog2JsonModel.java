package com.timevale.drc.base.binlog;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;


/**
 * binlog 转 json 模型.
 *
 * @author cxs
 */
@Getter
@Setter
@ToString
public class Binlog2JsonModel {

    public static final String ACTION_INSERT = "INSERT";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_DDL = "DDL";

    /**
     * INSERT(0, 1),
     * UPDATE(1, 2),
     * DELETE(2, 3),
     * DDL(2, 3),
     * 事件类型.
     */
    String action;
    /**
     * 库名.
     */
    String dbName;
    /**
     * 表名.
     */
    String tableName;
    /**
     * 主键.
     */
    String rowKey;

    /**
     * 新键值.
     */
    Map<String, Object> after;

    /**
     * 这是老版本的方式, 为了兼容老的 DRC 版本.
     */
    @Deprecated
    Map<String, Object> content;

    /**
     * 老数据
     */
    Map<String, Object> before;

    String gtId;

    String originalSql;

    public void setAfter(Map<String, Object> after) {
        this.after = after;
        this.setContent(after);
    }
}
