package com.timevale.drc.base.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

/**
 * @author gwk_2
 * @date 2021/1/29 00:10
 */
@Getter
@Setter
@ToString
public class BaseDO {

    public static final int DELETED_YES = 1;
    public static final int DELETED_NO = 0;

    @Id
    @GeneratedValue(generator = "JDBC")
    protected Integer id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    protected Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    protected Date updateTime;

    /**
     * 0 正常
     * 1 已经删除
     */
    protected Integer isDeleted;

}
