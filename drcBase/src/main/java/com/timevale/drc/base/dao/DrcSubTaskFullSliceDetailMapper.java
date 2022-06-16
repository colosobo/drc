package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/29 00:45
 * @description
 */
public interface DrcSubTaskFullSliceDetailMapper extends Mapper<DrcSubTaskFullSliceDetail>{

    @Select("select * from drc_sub_task_full_slice_detail where sub_task_name = #{name}")
    DrcSubTaskFullSliceDetail selectByName(String name);

    @Select("select * from drc_sub_task_full_slice_detail where drc_sub_task_full_config_id = #{fullConfigId}")
    List<DrcSubTaskFullSliceDetail> selectFullConfigId(Integer fullConfigId);

    @Select("select * from drc_sub_task_full_slice_detail where parent_id = #{parentId}")
    List<DrcSubTaskFullSliceDetail> selectByParentId(Integer parentId);

    @Select("select * from drc_sub_task_full_slice_detail where parent_id = #{parentId} and state = 1")
    List<DrcSubTaskFullSliceDetail> selectByParentIdAndRunning(Integer parentId);

    @Update("update drc_sub_task_full_slice_detail set state = #{state} where id = #{id}")
    Integer updateState(Integer state, Integer id);

    @Update("update drc_sub_task_full_slice_detail set state = #{state} where sub_task_name = #{subTaskName}")
    Integer updateStateByName(Integer state, String subTaskName);

    @Select("select count(1) from drc_sub_task_full_slice_detail  where state = 1")
    int selectRunning();

    @Select("select count(1) from drc_sub_task_full_slice_detail  where state = 1 and " +
            "parent_id = #{0}")
    int selectRunningCount(Integer parentId);

    @Select("select * from drc_sub_task_full_slice_detail where state = 0 order by parent_id")
    List<DrcSubTaskFullSliceDetail> selectAllStateInit();
}
