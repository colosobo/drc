package com.timevale.drc.worker.service.canal.support;

import com.alibaba.otter.canal.common.CanalException;
import com.alibaba.otter.canal.instance.core.CanalInstance;
import com.alibaba.otter.canal.instance.core.CanalInstanceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

/**
 * @author gwk_2
 * @date 2021/4/12 21:49
 */
@Slf4j
public class DrcCanalInstanceGenerator implements CanalInstanceGenerator {

    private String springXml = "classpath:spring/default-instance.xml";
    private String defaultName = "instance";
    private BeanFactory beanFactory;
    private Properties canalConfig;
    private String zkAddr;

    private ConcurrentHashMap<String, Properties> map = new ConcurrentHashMap<>();

    public DrcCanalInstanceGenerator(String zkAddr) {
        try {
            this.zkAddr = zkAddr;
            String conf = System.getProperty("canal.conf", "classpath:canal.properties");
            Properties properties = new Properties();
            if (conf.startsWith(CLASSPATH_URL_PREFIX)) {
                conf = StringUtils.substringAfter(conf, CLASSPATH_URL_PREFIX);
                properties.load(DrcCanalInstanceGenerator.class.getClassLoader().getResourceAsStream(conf));
            } else {
                properties.load(new FileInputStream(conf));
            }
            canalConfig = properties;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Properties addInstanceProperties(String key, Properties properties) {
        return map.put(key, properties);
    }

    @Override
    public CanalInstance generate(String destination) {
        synchronized (this) {
            try {
                Properties properties = map.get(destination);
                // merge local
                properties.putAll(canalConfig);

                // 设置动态properties,替换掉本地properties
                com.alibaba.otter.canal.instance.spring.support.PropertyPlaceholderConfigurer.propertiesLocal.set(properties);
                // 设置当前正在加载的通道，加载spring查找文件时会用到该变量
                System.setProperty("canal.instance.destination", destination);
                System.setProperty("canal.zkServers", zkAddr);

                this.beanFactory = getBeanFactory(springXml);

                String beanName = destination;
                if (!beanFactory.containsBean(beanName)) {
                    beanName = defaultName;
                }

                return (CanalInstance) beanFactory.getBean(beanName);
            } catch (Throwable e) {
                log.error("generator instance failed.", e);
                throw new CanalException(e);
            } finally {
                System.setProperty("canal.instance.destination", "");
                com.alibaba.otter.canal.instance.spring.support.PropertyPlaceholderConfigurer.propertiesLocal.set(null);
            }
        }
    }

    private BeanFactory getBeanFactory(String springXml) {
        return new ClassPathXmlApplicationContext(springXml);
    }
}
