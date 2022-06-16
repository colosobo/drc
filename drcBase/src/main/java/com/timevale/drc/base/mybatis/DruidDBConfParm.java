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

/**
 * 默认druid配置
 *
 */
public class DruidDBConfParm {

    public static final String DRIVER_CLASS_NAME                         = "com.mysql.jdbc.Driver";

    public static final String TYPE                                      = "com.alibaba.druid.pool.DruidDataSource";

    public static final String MAX_IDLE                                  = "10";

    public static final String MIN_IDLE                                  = "10";

    public static final String MAX_WAIT                                  = "3000";

    public static final String TIME_BETWEEN_EVICTION_RUNS_MILLIS         = "60000";

    public static final String MIN_EVICTABLE_IDLE_TIME_MILLIS            = "200000";

    public static final String VALIDATION_QUERY                          = "SELECT 1 FROM DUAL";

    public static final String TEST_WHILE_IDLE                           = "true";

    public static final String TEST_ON_BORROW                            = "false";

    public static final String TEST_ON_RETURN                            = "false";

    public static final String POOL_PREPARED_STATEMENTS                  = "true";

    public static final String MAXPOOLPREPAREDSTATEMENTPERCONNECTIONSIZE = "100";
}
