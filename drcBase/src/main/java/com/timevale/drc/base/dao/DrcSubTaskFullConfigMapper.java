package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/29 00:45
 * @description
 */
public interface DrcSubTaskFullConfigMapper extends Mapper<DrcSubTaskFullConfig>{

    @Select("select * from drc_sub_task_full_config where drc_task_id = #{drcTaskId} limit 1")
    DrcSubTaskFullConfig selectOneByDrcTaskId(Integer drcTaskId);

    @Select("select * from drc_sub_task_full_config where drc_task_id = #{drcTaskId}")
    List<DrcSubTaskFullConfig> selectListByDrcTaskId(Integer drcTaskId);

    @Update("update drc_sub_task_full_config set slice_count = #{newSliceCount} where slice_count = #{oldSliceCount} and id = #{id}")
    int updateSliceCount(Integer oldSliceCount, Integer newSliceCount, Integer id);

    @Update("update drc_sub_task_full_config set finish_slice_count = #{newSliceCount} where finish_slice_count = #{oldSliceCount} and id = #{id}")
    int updateFinishSliceCount(Integer oldSliceCount, Integer newSliceCount, Integer id);

    @Select("select * from drc_sub_task_full_config where split_state = 0 or split_state = 1")
    List<DrcSubTaskFullConfig> selectFailSplitTask();

    @Update("update drc_sub_task_full_config set split_state = 1 where id = #{0} ")
    int updateStateRunningById(Integer id);
}
