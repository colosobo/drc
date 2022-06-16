package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.QpsLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/4/16 17:29
 */
public interface QpsLogMapper extends Mapper<QpsLog> {

    @Delete("delete from drc_qps_log where time_in_seconds < #{timeInSeconds} ")
    int deleteWhenLessThanTime(Long timeInSeconds);

    @Select("select * from drc_qps_log where name = #{name} and time_in_seconds >= #{start} and  time_in_seconds <= #{end}")
    List<QpsLog> selectByTimeRange(String name, Long start, Long end);
}
