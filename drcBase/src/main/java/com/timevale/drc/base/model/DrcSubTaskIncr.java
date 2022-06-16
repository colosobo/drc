package com.timevale.drc.base.model;

import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.model.ext.DrcSubTaskIncrExt;
import com.timevale.drc.base.serialize.JackSonUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Table;

/**
 * @author gwk_2
 * @date 2021/3/2 10:52
 */
@Data
@Table(name = "drc_sub_task_incr")
public class DrcSubTaskIncr extends BaseTaskModel {

    private Integer dbConfigId;

    private String tableExpression;

    private String sinkJson;

    private String ext;

    public DrcSubTaskIncrBO toBO(){
        DrcSubTaskIncrBO drcSubTaskIncrBO = new DrcSubTaskIncrBO();
        drcSubTaskIncrBO.setId(this.id);
        drcSubTaskIncrBO.setCreateTime(this.createTime);
        drcSubTaskIncrBO.setUpdateTime(this.updateTime);
        drcSubTaskIncrBO.setIsDeleted(this.isDeleted);
        drcSubTaskIncrBO.setSubTaskName(this.subTaskName);
        drcSubTaskIncrBO.setParentId(this.parentId);
        drcSubTaskIncrBO.setState(this.state);
        drcSubTaskIncrBO.setDbConfigId(this.dbConfigId);
        drcSubTaskIncrBO.setTableExpression(this.tableExpression);
        drcSubTaskIncrBO.setSinkJson(this.sinkJson);
        if(StringUtils.isNotBlank(this.ext)) {
            drcSubTaskIncrBO.setDrcSubTaskIncrExt(JackSonUtil.string2Obj(this.ext, DrcSubTaskIncrExt.class));
        }else{
            drcSubTaskIncrBO.setDrcSubTaskIncrExt(new DrcSubTaskIncrExt());
        }
        return drcSubTaskIncrBO;
    }
}
