package com.timevale.drc.base.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 正常流程:
 * 1. 从模板读取 properties.
 * 2. 使用数据库配置修改 properties.
 * 3. 将 properties 转换成 string. 传输到 canal-admin 接口中.
 */
public class PropertiesUtil {

    /**
     * 从当前 classpath 加载 properties 文件.
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Properties load(String fileName) throws IOException {
        InputStream is = PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName);

        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }
}
