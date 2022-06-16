package com.timevale.drc.base.alarm;

import com.google.common.collect.Lists;
import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.util.DrcHttpClientUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 告警工具类;
 *
 * @author gwk_2
 * @date 2020/8/11 18:11
 */
public class AlarmClient {

    public static final String SUFFIX_PATH = "/monitor/v1/notice/batch";
    public static final String TEST_DOMAIN = "http://monitor-stable.tsign.cn" + SUFFIX_PATH;
    /** 这个是横天给的. */
    public static final String FROM = "a62c8d7768daea64cc85baa6e901c233";
    public static final String SUBJECT = "DRC CLUSTER 系统告警";
    public static final String APP = "DRC CLUSTER";

    private final String domain;
    private final String env;

    public AlarmClient(String domain, String env) {
        if (StringUtils.isEmpty(domain)) {
            domain = TEST_DOMAIN;
        }
        this.domain = domain;
        this.env = env;
    }

    public void alarm(String content, List<String> nickNames) throws Exception {
        AlarmModel alarmModel = new AlarmModel();
        alarmModel.usernames = nickNames;
        alarmModel.content = content + "\r\n 环境: " + env;
        alarmModel.type = NoticeTypeEnum.DING_CROP_CONVERSATION;

        String s = JackSonUtil.obj2String(Lists.newArrayList(alarmModel));
        DrcHttpClientUtil.postAndReturnString(domain, s);
    }

    @Data
    static class AlarmModel {

        public String from = AlarmClient.FROM; // 必填
        public List<String> usernames;// groups和username二选一，监控平台中的花名英文
        public String content; // 必填
        public String subject = AlarmClient.SUBJECT; // 必填
        public String app = AlarmClient.APP;// 有就填
        public NoticeTypeEnum type; // 必填，EMAIL、SMS、CALL、DING_CROP_CONVERSATION、DING_ROBOT
    }

    enum NoticeTypeEnum {
        /**
         * 邮件
         */
        EMAIL("邮件"),
        /** 测试环境,短信发不出去 */
        SMS("短信"),
        /** 这个电话, 打出去, 提示 0000, 请注意 */
        CALL("电话"),
        DING_CROP_CONVERSATION("工作通知"),
        DING_ROBOT("机器人");

        String desc;

        NoticeTypeEnum(String desc) {
            this.desc = desc;
        }
    }


}
