package com.timevale.drc.base.sinkConfig;

import com.timevale.drc.base.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gwk_2
 * @date 2021/4/22 14:14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySQLSinkConfig implements SinkConfig {

    public String url;
    public String username;
    public String pwd;
    public String tableName;
    public String database;
    public int messageFormatType;
    public TaskTypeEnum taskTypeEnum;

    public boolean supportLoopSync;

    @Override
    public int getMessageFormatType() {
        return messageFormatType;
    }

    @Override
    public SinkConfig taskType(TaskTypeEnum taskTypeEnum) {
        this.taskTypeEnum = taskTypeEnum;
        return this;
    }
}
