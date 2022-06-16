package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcTaskRegisterTable;
import com.timevale.drc.base.model.result.SelectIdleWorkerResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface DrcTaskRegisterTableMapper extends Mapper<DrcTaskRegisterTable> {

    @Select("select a.ip_port as ipPort, a.id, count(*) count \n" +
            "from drc_machine_register_table a \n" +
            "         left join drc_task_register_table b on a.id = b.worker_id \n" +
            "where a.type = 2 \n" +
            "group by a.id")
    List<SelectIdleWorkerResult> selectTaskCountGroupByWorkerId();

    @Delete("delete from drc_task_register_table where task_name = #{0}")
    int deleteByTaskName(String taskName);

    @Select("select * from drc_task_register_table where task_name = #{0}")
    DrcTaskRegisterTable selectByTaskName(String taskName);

    @Select("select * from drc_task_register_table where worker_id = #{0}")
    List<DrcTaskRegisterTable> selectByWorkerId(Long workerId);

    @Update("update drc_task_register_table set update_time = now(), ext = #{1} where task_name = #{0}")
    int updateByTaskName(String taskName, String ext);
}
