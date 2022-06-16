package com.timevale.drc.base.util;

/**
 * canal_instance_gtidon=true
 * canal_instance_connectionCharset=UTF-8
 * canal_instance_rds_secretkey=
 * canal_instance_filter_black_regex=
 * canal_instance_master_gtid=
 * canal_instance_master_address=127_0_0_1\:3306
 * canal_instance_rds_accesskey=
 * canal_instance_master_journal_name=
 * canal_instance_master_position=
 * canal_instance_master_timestamp=
 * canal_instance_rds_instanceId=
 * canal_instance_enableDruid=false
 * canal_mq_partition=0
 * canal_instance_tsdb_enable=true
 * canal_instance_dbPassword=canal
 * canal_mq_topic=example
 * canal_instance_filter_regex=_*\\__*
 * canal_instance_dbUsername=canal
 *
 * @author 莫那·鲁道
 * @date 2019-10-14-10:17
 */
public interface PropertiesKey {

    String canal_instance_master_address = "canal.instance.master.address";
    String canal_instance_master_gtid = "canal.instance.master.gtid";
    String canal_instance_dbUsername = "canal.instance.dbUsername";
    String canal_instance_dbPassword = "canal.instance.dbPassword";
    String canal_instance_filter_regex = "canal.instance.filter.regex";

}
