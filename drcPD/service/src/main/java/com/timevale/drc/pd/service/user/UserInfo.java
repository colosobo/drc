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
public class UserInfo {

    private String id = "";//花名拼音
    private String name;//姓名
    private String alias;//花名
    private List<GroupModel> groupList;//所在部门列表
    private String mobile;//手机号
    private String mail;//邮箱
    private String job;//职位
}
