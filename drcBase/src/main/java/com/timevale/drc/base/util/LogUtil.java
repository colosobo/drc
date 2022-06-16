package com.timevale.drc.base.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志, 写入本地磁盘, 用于网页展示.
 * 代码来自 canal 项目.
 */
@Slf4j
public class LogUtil {

    private static final String BASE_PATH = "./bizLog/";

    private final static ConcurrentHashMap<String, Logger> LOGGER_MAP_CACHE = new ConcurrentHashMap<>();

    public static Logger getLogger(String name) {
        Logger logger = LOGGER_MAP_CACHE.get(name);
        if (null != logger) {
            return logger;
        }
        return createNewLogger(name);
    }

    private static Logger createNewLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.removeAllAppenders();
        logger.setLevel(Level.INFO);
        logger.setAdditivity(false);
        RollingFileAppender appender = new RollingFileAppender();
        PatternLayout layout = new PatternLayout();
        String conversionPatten = "[%d] %p %t %c - %m%n";
        layout.setConversionPattern(conversionPatten);
        appender.setLayout(layout);
        appender.setFile(BASE_PATH + name + ".log");
        appender.setEncoding("utf-8");
        appender.setMaxBackupIndex(10);
        appender.setMaxFileSize("50MB");
        appender.setAppend(true);
        appender.activateOptions();
        logger.addAppender(appender);
        LOGGER_MAP_CACHE.put(name, logger);
        return logger;
    }

    public static String getLogText(String logName, int line) {
        try {
            if (StringUtils.isBlank(logName)) {
                return "empty";
            }
            String fileNme = BASE_PATH + "/" + logName + ".log";
            return FileUtils.readFileFromOffset(fileNme, line, "UTF-8");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static void deleteLogger(String logName) {
        LOGGER_MAP_CACHE.remove(logName);
        String fileNme = BASE_PATH + "/" + logName + ".log";
        File file = new File(fileNme);
        if (file.exists()) {
            file.delete();
        }
    }
}
