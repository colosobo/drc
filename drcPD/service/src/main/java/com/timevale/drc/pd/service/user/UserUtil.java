package com.timevale.drc.pd.service.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Base64;

/**
 * @author 莫那·鲁道
 * @date 2019-09-18-14:25
 */
public class UserUtil {

    public static final String token = "x-timevale-jwtcontent";
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static UserInfo parse(String token) throws IOException {

        UserInfo result = new UserInfo();
        if (StringUtils.isNotEmpty(token)) {
            byte[] userInfo = Base64.getUrlDecoder().decode(token);
            result = mapper.readValue(userInfo, UserInfo.class);
        }

        return result;
    }

}
