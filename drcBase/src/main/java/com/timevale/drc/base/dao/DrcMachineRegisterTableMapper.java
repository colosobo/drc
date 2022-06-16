package com.timevale.drc.base.dao;

import com.timevale.drc.base.model.DrcMachineRegisterTable;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface DrcMachineRegisterTableMapper extends Mapper<DrcMachineRegisterTable> {


    @Delete("delete from drc_machine_register_table where ip_port = #{0}")
    int deleteByIpPortProcess(String ipPortProcess);

    @Select("select * from drc_machine_register_table where ip_port = #{0}")
    DrcMachineRegisterTable selectByIpPort(String ipPortProcess);

    @Select("select ip_port from drc_machine_register_table where type = 1")
    List<String> selectAllPD();

    @Select("select * from drc_machine_register_table where type = 1")
    List<DrcMachineRegisterTable> selectAllPDModel();

    @Select("select * from drc_machine_register_table where type = 2")
    List<DrcMachineRegisterTable> selectAllWorker();

    @Select("select * from drc_machine_register_table where type = 2")
    List<DrcMachineRegisterTable> selectAllWorkerModel();

    @Update("update drc_machine_register_table set update_time = now() where ip_port = #{0}")
    int renew(String ipPort);

}
