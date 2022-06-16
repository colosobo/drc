package com.timevale.drc.worker.service.task.mysql.util;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.StringUtils;

/**
 * @author shanreng
 *
 * canalEntry解析工具类
 */
public class DrcCanalEntryParseUtil {

    public static boolean isRowsQueryLogEventEntry(CanalEntry.RowChange rowChangeEntry){
        if(!rowChangeEntry.getIsDdl() && rowChangeEntry.getEventType() == CanalEntry.EventType.QUERY
            && StringUtils.isNotEmpty(rowChangeEntry.getSql())){
            // 不是ddl，类型为query，含有实际的sql
            // 就认为是RowsQueryLogEvent
            return true;
        }else {
            return false;
        }
    }

    public static CanalEntry.RowChange parseRowChange(CanalEntry.Entry entry){
        try {
            // 解析rowChange
            return CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (Exception e) {
            throw new RuntimeException("ERROR parse rowChange, entry:" + entry.toString());
        }
    }
}
