package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcSubTaskIncr;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/29 00:45
 * @description
 */
public interface DrcSubTaskIncrMapper extends Mapper<DrcSubTaskIncr>{


    @Select("select * from drc_sub_task_incr  where sub_task_name = #{name}")
    DrcSubTaskIncr selectByName(String name);

    @Select("select * from drc_sub_task_incr  where parent_id = #{parentId}")
    DrcSubTaskIncr selectByParentId(Integer parentId);


    @Update("update drc_sub_task_incr set state = #{state}  where sub_task_name = #{name}")
    void updateStateByName(Integer state, String name);

    @Select("select count(1) from drc_sub_task_incr  where state = 1")
    int selectRunning();

    @Select("select b.* " +
            "from drc_task a " +
            "         join drc_sub_task_incr b on a.id = b.parent_id " +
            "where b.state = 1 " +
            "  and b.is_deleted = 0 ")
    List<DrcSubTaskIncr> selectRunningIncrTask();
}
