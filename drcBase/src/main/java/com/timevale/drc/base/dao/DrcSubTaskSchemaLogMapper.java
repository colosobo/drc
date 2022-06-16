package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcSubTaskSchemaLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author gwk_2
 */
public interface DrcSubTaskSchemaLogMapper extends Mapper<DrcSubTaskSchemaLog> {

    @Select("select * from drc_sub_task_schema_log where parent_id = #{taskId}")
    DrcSubTaskSchemaLog selectByParentTaskId(Integer parentId);

    @Update("update drc_sub_task_schema_log set split_finish = #{current} where split_finish = #{pre} and id = #{id}")
    int updateTableFinish(Integer pre, Integer current, Integer id);

    @Delete("delete from drc_sub_task_schema_log where parent_id = #{parentId}")
    int deleteByParentId(Integer parentId);

    @Update("update drc_sub_task_schema_log set table_split_finish = #{current} where table_split_finish = #{pre} and id = #{id}")
    int updateTableSplitFinish(Integer pre, Integer current, Integer id);

}
