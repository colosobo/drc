package com.timevale.drc.pd.service.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author 莫那·鲁道
 * @date 2019-09-18-14:29
 */
@Getter
@Setter
public class GroupModel {

    private String groupId;//部门id
    private String groupName;//部门名称
    private String parentId;//父部门id
    private List<String> managerList;//部门主管id列表
    private String parentName;//父部门名称
}
