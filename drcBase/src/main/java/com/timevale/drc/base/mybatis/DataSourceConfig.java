/*
 *    Copyright the original author.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.timevale.drc.base.mybatis;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

/**
 * 数据源参数配置
 *
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
public class DataSourceConfig implements EnvironmentAware {

    /** logger */
    private Logger                  logger = LoggerFactory.getLogger(DataSourceConfig.class);

    private RelaxedPropertyResolver mybatisResolver;
    public Environment             environment;
    public RelaxedPropertyResolver propertyResolver;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.propertyResolver = new RelaxedPropertyResolver(environment, "spring.datasource.");
        this.mybatisResolver = new RelaxedPropertyResolver(environment, "mybatis.");
    }

    /**
     * 注册dataSource
     *
     * @return
     * @throws SQLException
     */
    @Bean(initMethod = "init", destroyMethod = "close")
    public DruidDataSource dataSource() throws SQLException {
        if (StringUtils.isEmpty(propertyResolver.getProperty("url"))) {
            logger.error("Your database connection pool configuration is incorrect!"
                         + " Please check your Spring profile, current profiles are:"
                         + Arrays.toString(environment.getActiveProfiles()));
            throw new ApplicationContextException(
                "Database connection pool is not configured correctly!");
        }
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("driverClassName"), DruidDBConfParm.DRIVER_CLASS_NAME));
        druidDataSource.setUrl(propertyResolver.getProperty("url"));
        druidDataSource.setUsername(propertyResolver.getProperty("username"));
        druidDataSource.setPassword(propertyResolver.getProperty("password"));
        druidDataSource
            .setInitialSize(Integer.parseInt(propertyResolver.getProperty("initialSize")));
        druidDataSource.setMaxActive(Integer.parseInt(propertyResolver.getProperty("maxActive")));
        druidDataSource.setMinIdle(Integer.parseInt(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("minIdle"), DruidDBConfParm.MIN_IDLE)));
        druidDataSource.setMaxWait(Integer.parseInt(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("maxWait"), DruidDBConfParm.MAX_WAIT)));
        druidDataSource.setTimeBetweenEvictionRunsMillis(Long.parseLong(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("timeBetweenEvictionRunsMillis"),
            DruidDBConfParm.TIME_BETWEEN_EVICTION_RUNS_MILLIS)));
        druidDataSource.setMinEvictableIdleTimeMillis(Long.parseLong(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("minEvictableIdleTimeMillis"),
            DruidDBConfParm.MIN_EVICTABLE_IDLE_TIME_MILLIS)));
        druidDataSource.setValidationQuery(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("validationQuery"), DruidDBConfParm.VALIDATION_QUERY));
        druidDataSource.setTestWhileIdle(Boolean.parseBoolean(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("testWhileIdle"), DruidDBConfParm.TEST_WHILE_IDLE)));
        druidDataSource.setTestOnBorrow(Boolean.parseBoolean(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("testOnBorrow"), DruidDBConfParm.TEST_ON_BORROW)));
        druidDataSource.setTestOnReturn(Boolean.parseBoolean(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("testOnReturn"), DruidDBConfParm.TEST_ON_RETURN)));
        druidDataSource.setPoolPreparedStatements(Boolean.parseBoolean(StringUtils.defaultIfBlank(
            propertyResolver.getProperty("poolPreparedStatements"),
            DruidDBConfParm.POOL_PREPARED_STATEMENTS)));
        druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(Integer.parseInt(StringUtils
            .defaultIfBlank(
                propertyResolver.getProperty("maxPoolPreparedStatementPerConnectionSize"),
                DruidDBConfParm.MAXPOOLPREPAREDSTATEMENTPERCONNECTIONSIZE)));
        //druidDataSource.setFilters(propertyResolver.getProperty("filters"));
        return druidDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());
        //mybatis分页
        PageHelper pageHelper = new PageHelper();
        Properties props = new Properties();
        props.setProperty("dialect", "mysql");
        props.setProperty("reasonable", "true");
        props.setProperty("supportMethodsArguments", "true");
        props.setProperty("returnPageInfo", "check");
        props.setProperty("params", "count=countSql;pageNum=start;pageSize=limit;");
        pageHelper.setProperties(props); //添加插件
        sqlSessionFactoryBean.setPlugins(new Interceptor[] { pageHelper });
        sqlSessionFactoryBean.setTypeAliasesPackage(mybatisResolver
            .getProperty("typeAliasesPackage"));
        sqlSessionFactoryBean.setConfigLocation(new DefaultResourceLoader()
            .getResource(mybatisResolver.getProperty("configLocation")));

        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws SQLException {
        return new DataSourceTransactionManager(dataSource());
    }

    //@Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        mapperScannerConfigurer.setBasePackage(mybatisResolver.getProperty("basePackage"));
        return mapperScannerConfigurer;
    }
}
