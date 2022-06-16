package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcTask;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/29 00:45
 * @description
 */
public interface DrcTaskMapper extends Mapper<DrcTask>{

    @Select("select * from drc_task where task_name = #{name} and is_deleted = 0")
    DrcTask selectByName(String name);

    @Select("select * from drc_task where is_deleted = 0 order by create_time desc")
    List<DrcTask> list();

    @Select("select * from drc_task where is_deleted = 0 and task_name like CONCAT('%',#{taskName},'%') order by create_time desc ")
    List<DrcTask> listByName(String taskName);

}
