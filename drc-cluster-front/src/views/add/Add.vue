<template>
  <PageContent v-loading="submitting" element-loading-text="表单提交中">

    <div style="background: white" class="add-box">
      <el-collapse>
        <el-collapse-item title="点击查看架构图" name="1">
          <img style="background: white; width: 60%; margin-left: 7%; text-align: center" src="../../assets/drc.png">
        </el-collapse-item>
      </el-collapse>
      <Card style="background: white" title="任务基本信息">
        <el-form
          :model="taskFrom" :rules="taskFromRules" ref="taskFromRef" label-width="160px"
          class="demo-ruleForm" label-position="right">
          <el-form-item label="任务类型" prop="type">
            <el-select v-model="taskFrom.type" placeholder="请选择" @change="handleChange">
              <el-option
                v-for="item in options"
                :key="item.value"
                :label="item.label"
                :value="item.value">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="任务全局唯一Key" prop="taskName">
            <el-input v-model="taskFrom.taskName" maxlength="25" minlength="3"
                      placeholder="只能是英文,下划线,不能有特殊符号"></el-input>
          </el-form-item>
          <el-form-item label="任务描述" prop="desc">
            <el-input v-model="taskFrom.desc" maxlength="20" placeholder="中文描述"></el-input>
          </el-form-item>

          <el-form-item label="表名列表" prop="dataBase.tableName" v-if="[4].includes(taskFrom.type)">
            <el-input type="textarea" placeholder="表名列表,多张表,换行分隔,*表示同步所有," v-model="taskFrom.tableName"/>
          </el-form-item>

          <el-form-item label="表名过滤表达式" prop="tableExpression" v-if="[1].includes(taskFrom.type)">
            <el-input placeholder="库名.表名(多张表, 使用逗号分隔, 可点击右方🔍查看规则)" v-model="taskFrom.tableExpression"></el-input>
            <el-button icon="el-icon-search" v-on:click="tableFilterDialog = true" circle></el-button>
          </el-form-item>

          <el-form-item label="表名" prop="tableName" v-if="[2, 3].includes(taskFrom.type)">
            <el-input v-model="taskFrom.tableName"></el-input>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 全量任务, 必须要有主键.</p>
          </el-form-item>

          <el-collapse>
            <el-collapse-item title="高级选项" name="1">
              <el-form-item label="每个分片的大致长度" prop="rangeSizeConfig"
                            v-if="[2, 3, 4].includes(taskFrom.type)">
                <el-input v-model="taskFrom.rangeSizeConfig"
                          placeholder="如果每个全量分片100w条数据, 如果总数是1亿条,就是100个分片"></el-input>
                <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 单位万</p>
              </el-form-item>

              <el-form-item label="最大 QPS 限制" prop="qpsLimitConfig">
                <el-input v-model="taskFrom.qpsLimitConfig" placeholder="默认单个任务最大1000QPS"></el-input>
                <br>
                <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: QPS 过大,可能会冲垮您的目标数据源.提交后可随时修改且生效,建议初始值小一点,根据数据库 CPU 情况进行 QPS 参数调节. </p>
              </el-form-item>

              <el-row>
                <el-col :span="12">
                  <el-form-item label="是否支持DDL同步" prop="supportDDLSyncConfig" v-if="[1,3,4].includes(taskFrom.type)">
                    <el-switch v-model="taskFrom.supportDDLSync"></el-switch>
                    <br>
                    <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意：大表的DDL语句会很耗时</p>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="DDL同步时是否过滤DML语句" prop="supportDDLSyncConfig" label-width="240px"
                                v-if="taskFrom.supportDDLSync">
                    <el-switch v-model="taskFrom.DDLSyncFilterDML"></el-switch>
                    <br>
                    <p style="color: darkred ; font-size: 10px; line-height:12px ;"></p>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="全量时where条件语句" prop="rangeSizeConfig"
                            v-if="[2, 3].includes(taskFrom.type)">
                <el-input v-model="taskFrom.whereStatement"
                          placeholder="where 语句, 用于指定某些 id, 或者某些条件. "></el-input>
                <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 例如 where id > 100 and id < 999,
                  语句不能包含 order by 和 limit</p>
              </el-form-item>

            </el-collapse-item>
          </el-collapse>
        </el-form>
      </Card>
      <Card style="background: white" title="主库信息（增量同步）" v-if="[1, 3, 4].includes(taskFrom.type)">
        <el-form
          :model="incrFrom" :rules="incrFromRules" key="incrFromKey" ref="incrFromRef"
          label-width="160px"
          class="demo-ruleForm">
          <el-form-item label="url" prop="url" key="incrFrom.url">
            <el-input placeholder="IP:3306" v-model="incrFrom.url"></el-input>
          </el-form-item>
          <el-form-item label="username" prop="username" key="incrFrom.username">
            <el-input v-model="incrFrom.username"></el-input>
          </el-form-item>
          <el-form-item label="password" prop="pwd" key="incrFrom.pwd">
            <el-input type="password" v-model="incrFrom.pwd"></el-input>
          </el-form-item>
          <el-form-item label="dataBase name" prop="database" key="incrFrom.database">
            <el-input v-model="incrFrom.database"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="testDatabase('incrFromRef')">测试数据库连接
            </el-button>
          </el-form-item>
        </el-form>
      </Card>
      <Card style="background: white" title="读库信息（全量同步）" v-if="[3, 2, 4].includes(taskFrom.type)">
        <el-form
          :model="totalFrom" :rules="totalFromRules" ref="totalFromRef" label-width="160px"
          class="demo-ruleForm">
          <el-form-item label="url" prop="url">
            <el-input placeholder="IP:3306" v-model="totalFrom.url"></el-input>
          </el-form-item>
          <el-form-item label="username" prop="username">
            <el-input v-model="totalFrom.username"></el-input>
          </el-form-item>
          <el-form-item label="password" prop="pwd">
            <el-input type="password" v-model="totalFrom.pwd"></el-input>
          </el-form-item>
          <el-form-item label="dataBase name" prop="database">
            <el-input v-model="totalFrom.database"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="testDatabase('totalFromRef')">测试数据库连接
            </el-button>
          </el-form-item>
        </el-form>
      </Card>
      <Card style="background: white" title="Sink 配置">
        <el-form
          :model="customSinkForm"
          ref="customSinkFormRef"
          label-width="160px"
          class="demo-ruleForm">
<!--          <el-form-item label="是否启用自定义Sink" prop="useCustomSink">-->
<!--            <el-switch v-model="customSinkForm.useCustomSink"></el-switch>-->
<!--            <span class="add-box__tip">-->
<!--              *如果不自定义，默认投递到阿里云 Kafka 中, 如果数据量很大, 阿里云 kafka 可能需要扩容, 请注意.</span>-->
<!--          </el-form-item>-->
          <el-form-item label="Sink类型" prop="customSinkType" v-show="customSinkForm.useCustomSink">
            <el-radio-group v-model="customSinkForm.customSinkType">
              <el-radio-button v-for="type in customSinkTypes" :label="type" :key="type"></el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <!-- CanalKafka -->
        <el-form
          v-show="showCustomSinkSetting('CanalKafka')"
          ref="KafkaFormRef"
          :model="CanalKafkaForm"
          label-width="160px"
          class="demo-ruleForm">

          <el-form-item label="Kafka地址" prop="kafkaBootstrapServers">
            <el-input placeholder="bootstrap.servers" v-model="CanalKafkaForm.kafkaBootstrapServers"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 逗号分隔多个地址, 需要确保地址正确</p>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: canal kafka 会使用原生的 Message 格式进行投递, 和 DRC
              格式不同.</p>
          </el-form-item>

          <el-form-item label="队列编号" prop="partition" v-if="true">
            <el-input placeholder="最好是0" v-model="CanalKafkaForm.partition"></el-input>
          </el-form-item>

          <el-form-item label="topic名称" prop="topic">
            <el-input placeholder="topic名称" v-model="CanalKafkaForm.topic"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">
              注意: 需要先创建好您的 topic(建议加上 DRC_DS_ 前缀)</p>
          </el-form-item>

        </el-form>

        <!-- Kafka -->
        <el-form
          v-show="showCustomSinkSetting('Kafka')"
          ref="KafkaFormRef"
          :model="KafkaForm"
          :rules="KafkaFormRules"
          label-width="160px"
          class="demo-ruleForm">
          <el-form-item label="Kafka地址" prop="kafkaBootstrapServers">
            <el-input placeholder="bootstrap.servers" v-model="KafkaForm.kafkaBootstrapServers"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 逗号分隔多个地址, 需要确保地址正确</p>
          </el-form-item>
          <el-form-item label="topic名称" prop="topic">
            <el-input placeholder="topic名称" v-model="KafkaForm.topic"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">
              注意: 需要先创建好您的 topic(建议加上 DRC_DS_ 前缀)</p>
          </el-form-item>

          <el-collapse>
            <el-collapse-item title="高级选项" name="1">
              <el-form-item label="是否异步发送" prop="isAsync">
                <el-switch v-model="KafkaForm.isAsync"></el-switch>
                <br>
                <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 异步发送, 重启机器可能会导致丢失数据, 但相对同步性能会有 10
                  倍的提升.</p>
              </el-form-item>
              <el-form-item label="保持绝对有序" prop="oncePartitionEnabled">
                <el-switch v-model="KafkaForm.oncePartitionEnabled"></el-switch>
                <br>
                <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 如果选择绝对有序, 消息仅会进入到一个队列,
                  这可能会导致性能问题和可用性问题</p>
              </el-form-item>
              <el-form-item label="队列编号" prop="partition" v-if="KafkaForm.oncePartitionEnabled">
                <el-input placeholder="最好是0" v-model="KafkaForm.partition"></el-input>
              </el-form-item>
              <el-form-item label="key.serializer" prop="keySerializer">
                <el-input placeholder="org.apache.kafka.common.serialization.String-Serializer"
                          v-model="KafkaForm.keySerializer"></el-input>
              </el-form-item>
              <el-form-item label="value.serializer" prop="valueSerializer">
                <el-input placeholder="org.apache.kafka.common.serialization.String-Serializer"
                          v-model="KafkaForm.valueSerializer"></el-input>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>

        </el-form>

        <!-- MySQL -->
        <el-form
          v-show="showCustomSinkSetting('MySQL')"
          :model="MySQLForm"
          :rules="MySQLFormRules"
          ref="MySQLFormRef"
          label-width="160px"
          class="demo-ruleForm">
          <el-form-item label="url" prop="url">
            <el-input placeholder="ip:3306" v-model="MySQLForm.url"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">
              注意: 1. DRC 会自动进行表结构迁移和索引迁移.
              <br>
              2. 如果任务类型是 MySQL 增量同步, 使用 MySQL 作为 Sink 的话, 该 Sink 无法支持多表. 仅支持单表.
            </p>
          </el-form-item>

          <el-form-item label="username" prop="username">
            <el-input placeholder="用户名" v-model="MySQLForm.username"></el-input>
          </el-form-item>
          <el-form-item label="password" prop="pwd">
            <el-input type="password" placeholder="密码" v-model="MySQLForm.pwd"></el-input>
          </el-form-item>
          <el-form-item label="database Name" prop="database">
            <el-input placeholder="" v-model="MySQLForm.database"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">
              注意: 需要先创建好您的 Database</p>
          </el-form-item>

          <el-form-item label="tableName" prop="tableName" v-if="[1,2,3].includes(taskFrom.type)" >
            <el-input placeholder="" v-model="MySQLForm.tableName"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">不是必填, 如果填了, 当单表同步时, 使用单表同步时, 会使用此表名</p>
          </el-form-item>

          <el-form-item label="是否支持回环同步">
            <el-switch v-model="MySQLForm.supportLoopSync"></el-switch>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="small" @click="testDatabase('MySQLForm')">测试数据库连接
            </el-button>
          </el-form-item>
        </el-form>

        <!-- RocketMQ -->
        <el-form
          v-show="showCustomSinkSetting('RocketMQ')"
          :model="RocketMQForm"
          :rules="RocketMQFormRules"
          ref="RocketMQFormRef"
          label-width="160px"
          class="demo-ruleForm">
          <el-form-item label="nameServer地址" prop="nameServer">
            <el-input placeholder="nameServer" v-model="RocketMQForm.nameServer"></el-input>
          </el-form-item>
          <el-form-item label="topic名称" prop="topic">
            <el-input placeholder="topic名称" v-model="RocketMQForm.topic"></el-input>
            <br>
            <p style="color: darkred ; font-size: 10px; line-height:12px ;">
              注意: 1. 需要先创建好您的 topic, 如果需要绝对有序, 需要将 rocketmq 的读写队列数字设置为 1, 否则可能导致乱序
              <br>
              注意: 2. 原 DRC 迁移, topic 需要加上 DRC_DS_ 前缀
            </p>
          </el-form-item>
          <el-collapse>
            <el-collapse-item title="高级选项" name="1">
              <el-form-item label="tag名称" prop="tag">
                <el-input placeholder="tag" v-model="RocketMQForm.tag"></el-input>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </el-form>

        <!-- HBaseCE -->
        <el-form
          v-show="showCustomSinkSetting('HBaseCE')"
          :model="HBaseCEForm"
          label-width="160px"
          class="demo-ruleForm"
        >
          <el-form-item label="HBase表名" prop="tableName">
            <el-input placeholder="tableName" v-model="HBaseCEForm.tableName"></el-input>
          </el-form-item>
          <el-form-item label="zookeeperQuorum" prop="zookeeperQuorum">
            <el-input placeholder="zookeeperQuorum ZK地址，集群url用逗号隔开" v-model="HBaseCEForm.zookeeperQuorum"></el-input>
          </el-form-item>
          <el-form-item label="rowKey生成规则" prop="rowKeyGeneratorExp">
            <el-input placeholder="rowKey生成规则 （多个字段拼接用逗号隔开）" v-model="HBaseCEForm.rowKeyGeneratorExp"></el-input>
          </el-form-item>
          <el-button size="small" @click="pullMysqlTableMetaData">获取mysql表结构数据</el-button>

          <el-table
            :data="HBaseCEForm.hBaseMappingConfig.columnMappingConfig"
            style="width: 100%" :border="true" align="center"
          >
            <el-table-column type="index" width="50" align="center"></el-table-column>
            <el-table-column label="mysql列名" align="center">
              <template slot-scope="scope">
                <span style="margin-left: 10px">{{ scope.row.columnName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="mysql列类型" align="center">
              <template slot-scope="scope">
                <span style="margin-left: 10px">{{ scope.row.columnType }}</span>
              </template>
            </el-table-column>

            <el-table-column label="hbase列族" align="center">
              <template slot-scope="scope">
                <el-input placeholder="hbase列族" v-model="scope.row.hbaseColumnFamily" ></el-input>
              </template>
            </el-table-column>

            <el-table-column label="hbase列名" align="center">
              <template slot-scope="scope">
                <el-input placeholder="hbase列名" v-model="scope.row.hbaseColumnName"></el-input>
              </template>
            </el-table-column>

            <el-table-column label="hbase列名" align="center">
              <template slot-scope="scope">
                <el-select v-model="scope.row.hbaseNativeType" placeholder="请选择">
                  <el-option
                    v-for="item in hbaseNativeTypeSelectOptions"
                    :key="item.value" :label="item.label" :value="item.value">
                  </el-option>
                </el-select>
              </template>
            </el-table-column>

          </el-table>

        </el-form>

      </Card>
      <div class="btn-box">
        <el-button size="small" @click="resetData">重置</el-button>
        <el-button size="small" type="primary" @click="submit">提交</el-button>
        <p style="color: darkred ; font-size: 10px; line-height:12px ;">注意: 只有当纯增量任务时,才能使用 CanalKafka Sink</p>
      </div>

      <el-dialog
        title="提示"
        :visible.sync="tableFilterDialog"
        width="30%">
      <span>
        例子：<br>
        0. 订阅单张表: databaseName.tableName
        <br>
        1. 订阅多张表：databaseName.tableName111,databaseName.tableName2222 (逗号分隔)
        <br>
        2. 也可使用 databaseName.* , 订阅该 database 的所有表.
      </span>
        <span slot="footer" class="dialog-footer">
      </span>
      </el-dialog>
    </div>

  </PageContent>

</template>

<script>
import Card from '@/components/Card'

export default {
  name: 'Add',
  components: {Card},
  created() {
    this.buildData()
    // this.getTableDataTimer = setInterval(this.getTableData, 2000)
  },
  data() {

    return {

      options: [
        {
          value: 4,
          label: 'MySQL 库全量 + MySQL 库增量'
        },
        {
          value: 3,
          label: 'MySQL 单表全量 + MySQL 单表增量'
        },
        {
          value: 2,
          label: 'MySQL 单表全量任务'
        },
        {
          value: 1,
          label: 'MySQL 增量'
        },
      ],

      hbaseNativeTypeSelectOptions: [
        {value: "STRING", label: "STRING"},
        {value: "INTEGER", label: "INTEGER"},
        {value: "LONG", label: "LONG"},
        {value: "SHORT", label: "SHORT"},
        {value: "BOOLEAN", label: "BOOLEAN"},
        {value: "FLOAT", label: "FLOAT"},
        {value: "DOUBLE", label: "DOUBLE"},
        {value: "BIG_DECIMAL", label: "BIG_DECIMAL"},
        {value: "DATE", label: "DATE"},
        {value: "BYTE", label: "BYTE"},
        {value: "BYTES", label: "BYTES"}
      ],

      // 任务信息
      taskFrom: {
        type: 1,
        taskName: '',
        desc: '',
        qpsLimitConfig: '2000',
        supportDDLSync: false,
        DDLSyncFilterDML: false,
        // 过滤表达式
        tableExpression: '',
        tableName: '',
        rangeSizeConfig: '500',
        whereStatement: "",
        selectFieldList: '*',
        sinkType: false,
        kafkaSinkConfig: {},
        mySQLSinkConfig: {},
        rocketSinkConfig: {},
      },
      taskFromRules: {
        type: {required: true, message: '请输入', trigger: 'change'},
        taskName: {required: true, message: '请输入', trigger: 'blur'},
        desc: {required: true, message: '请输入', trigger: 'blur'},
        qpsLimitConfig: {required: true, message: '请输入', trigger: 'blur'},
        supportDDLSyncConfig: {trigger: 'blur'},

        // 过滤表达式
        tableExpression: {required: true, message: '请输入', trigger: 'blur'},
        tableName: {required: true, message: '请输入', trigger: 'blur'},
        rangeSizeConfig: {required: true, message: '请输入', trigger: 'blur'},
        selectFieldList: {required: true, message: '请输入', trigger: 'blur'},
      },
      // 增量
      incrFrom: {
        database: '',
        pwd: '',
        url: '',
        username: ''
      },
      incrFromRules: {
        database: {required: true, message: '请输入', trigger: 'blur'},
        pwd: {required: true, message: '请输入', trigger: 'blur'},
        url: {required: true, message: '请输入', trigger: 'blur'},
        username: {required: true, message: '请输入', trigger: 'blur'},
      },
      tableFilterDialog: false,
      // 全量
      totalFrom: {
        database: '',
        pwd: '',
        url: '',
        username: ''
      },
      totalFromRules: {
        database: {required: true, message: '请输入', trigger: 'blur'},
        pwd: {required: true, message: '请输入', trigger: 'blur'},
        url: {required: true, message: '请输入', trigger: 'blur'},
        username: {required: true, message: '请输入', trigger: 'blur'},
      },
      CanalKafkaForm: {
        kafkaBootstrapServers: '192.168.2.128:6667',
        topic: 'DRC_DS_',
        partition: '0',
        messageFormatType: 2
      },
      KafkaForm: {
        kafkaBootstrapServers: '192.168.2.128:6667',
        topic: 'DRC_DS_',
        oncePartitionEnabled: false,
        partition: '',
        keySerializer: 'org.apache.kafka.common.serialization.StringSerializer',
        valueSerializer: 'org.apache.kafka.common.serialization.StringSerializer',
        isAsync: false
      },
      MySQLForm: {
        url: '',
        database: '',
        username: '',
        pwd: '',
        batch:false,
        tableName:'',
        supportLoopSync: false,
      },
      RocketMQForm: {
        nameServer: '172.20.62.133:9876',
        topic: 'DRC_DS_',
        tag: ''
      },
      HBaseCEForm:{
        tableName: '',
        zookeeperQuorum: '',
        hBaseMappingConfig: {
          rowKeyGeneratorExp : '',
          columnMappingConfig : [{
            "columnName" : '',
            "columnType" : '',
            "hbaseColumnFamily" : '',
            "hbaseColumnName" : '',
            "hbaseNativeType" : ''
          }]
        },
      },

      KafkaFormRules: {
        kafkaBootstrapServers: {required: true, message: '请输入', trigger: 'blur'},
        topic: {required: true, message: '请输入', trigger: 'blur'},
        partition: {required: true, message: '请输入', trigger: 'blur'},
        keySerializer: {required: true, message: '请输入', trigger: 'blur'},
        valueSerializer: {required: true, message: '请输入', trigger: 'blur'},
      },
      MySQLFormRules: {
        url: {required: true, message: '请输入', trigger: 'blur'},
        database: {required: true, message: '请输入', trigger: 'blur'},
        username: {required: true, message: '请输入', trigger: 'blur'},
        pwd: {required: true, message: '请输入', trigger: 'blur'},
      },
      RocketMQFormRules: {
        nameServerUrl: {required: true, message: '请输入', trigger: 'blur'},
        topicName: {required: true, message: '请输入', trigger: 'blur'},
      },
      customSinkTypes: ['Kafka', 'MySQL', 'RocketMQ', "CanalKafka", "HBaseCE"],
      customSinkForm: {
        useCustomSink: true,
        customSinkType: 'RocketMQ'
      },
      submitting: false,
    }
  },
  methods: {
    showCustomSinkSetting(type) {
      // this.RocketMQForm.topic = 'DRC_DS_' + this.taskFrom.taskName
      // this.CanalKafkaForm.topic = 'DRC_DS_' + this.taskFrom.taskName
      // this.KafkaForm.topic = 'DRC_DS_' + this.taskFrom.taskName;
      if (type === "CanalKafka") {
        return this.customSinkForm.useCustomSink && this.customSinkForm.customSinkType === type && this.taskFrom.type === 1;
      }

      if (type === "HBaseCE") {
        return false;
        // var showHBaseCE = this.customSinkForm.useCustomSink && this.customSinkForm.customSinkType === type && this.taskFrom.type === 2;
        // console.log("showHBaseCE:" + showHBaseCE);
        // return showHBaseCE;
      }

      return this.customSinkForm.useCustomSink && this.customSinkForm.customSinkType === type;
    },
    async submit() {
      try {
        const fromList = []
        fromList.push(this.$refs.taskFromRef.validate())

        this.$refs.incrFromRef && fromList.push(this.$refs.incrFromRef.validate())

        this.$refs.totalFromRef && fromList.push(this.$refs.totalFromRef.validate())
        if (this.customSinkForm.useCustomSink) {
          const sinkFormRef = this.$refs[`${this.customSinkForm.customSinkType}FormRef`]
          sinkFormRef && fromList.push(sinkFormRef.validate())
        }
        await Promise.all(fromList)
        this.submitting = true
        const {type} = this.taskFrom
        this.taskFrom.sinkType = 0
        if (this.customSinkForm.useCustomSink) {
          if (this.customSinkForm.customSinkType === "Kafka") {
            this.taskFrom.sinkType = 1
          }
          if (this.customSinkForm.customSinkType === "MySQL") {
            this.taskFrom.sinkType = 2
          }
          if (this.customSinkForm.customSinkType === "RocketMQ") {
            this.taskFrom.sinkType = 3
          }
          if (this.customSinkForm.customSinkType === "CanalKafka") {
            if (this.taskFrom.type !== 1) {
              this.$message.warning('当选择 CanalKafka 时, 必须配合使用纯增量任务.')
              return;
            }
            this.taskFrom.sinkType = 4;
          }
        }
        this.taskFrom.kafkaSinkConfig = this.KafkaForm;
        this.taskFrom.mySQLSinkConfig = this.MySQLForm;
        this.taskFrom.rocketSinkConfig = this.RocketMQForm;
        this.taskFrom.canalKafkaSinkConfig = this.CanalKafkaForm;

        // 增量
        if (type === 1) {
          await this.$Server('/task/addIncr', 'POST', {
            dbConfigVO: {...this.incrFrom},
            drcTaskVO: {...this.taskFrom},
            drcSubTaskIncrVO: {...this.taskFrom},
          })
        }

        // 全量
        if (type === 2) {
          await this.$Server('/task/addFull', 'POST', {
            drcTaskVO: {...this.taskFrom},
            dbConfigVO: {...this.totalFrom},
            fullTaskConfigVO: {...this.taskFrom},
          })
        }

        // 混合
        if (type === 3) {
          await this.$Server('/task/addMix', 'POST', {
            drcSubTaskIncrVO: {...this.taskFrom},
            drcTaskVO: {...this.taskFrom},
            fullDbConfig: {...this.totalFrom},
            fullTaskConfigVO: {...this.taskFrom},
            incrDbConfig: {...this.incrFrom}
          })
        }

        // 混合
        if (type === 4) {
          await this.$Server('/task/addDataBaseMix', 'POST', {
            drcSubTaskIncrVO: {...this.taskFrom},
            drcTaskVO: {...this.taskFrom},
            fullDbConfig: {...this.totalFrom},
            fullTaskConfigVO: {...this.taskFrom},
            incrDbConfig: {...this.incrFrom}
          })
        }

        this.$message.success('添加成功')

        setTimeout(() => {
          this.$router.push({
            path: '/task'
          })
        }, 300)
      } catch (e) {
        console.log(e)
      } finally {
        this.submitting = false
        console.log('finally')
      }
    },
    async testDatabase(ref) {

      if (!ref) return

      try {
        try {
          await this.$refs[ref].validate()
        } catch (e) {
          console.log(e)
        }

        var params = null
        if (ref === 'incrFromRef') {
          params = this.incrFrom;
        }
        if (ref === 'totalFromRef') {
          params = this.totalFrom;
        }
        if (ref === 'MySQLForm') {
          params = this.MySQLForm;
        }

        const res = await this.$Server('/task/checkDataSourceValid', 'POST', params);

        console.log('res', res)

        this.$message.success('测试通过')

      } catch (e) {
        console.log(e)
      }
    },
    async pullMysqlTableMetaData(){
      console.log('pullMysqlTableMetaData');

      try{
        var params = {
          tableName : this.taskFrom.tableName,
          dbConfigVO : this.totalFrom
        };
        const res = await this.$Server('/task/pullMysqlTableMetaData', 'POST', params);
        var resultObject = res.data.resultObject;
        this.HBaseCEForm.hBaseMappingConfig.columnMappingConfig = resultObject.columnMetaDataList;

        this.HBaseCEForm.hBaseMappingConfig.columnMappingConfig.forEach(function (value){
          // 设置默认值
          value.hbaseColumnFamily = "f";
          value.hbaseColumnName = value.columnName;
        });
      }catch (e){
        console.log(e)
      }
    },

    handleChange() {
      console.log('handleChange')
      this.$refs.taskFromRef.clearValidate()

      this.$refs.incrFromRef && this.$refs.incrFromRef.clearValidate()

      this.$refs.totalFromRef && this.$refs.totalFromRef.clearValidate()
    },
    resetData() {
      const {type} = this.taskFrom

      this.$refs.taskFromRef.resetFields()

      this.taskFrom.type = type

      this.$refs.incrFromRef && this.$refs.incrFromRef.resetFields()

      this.$refs.totalFromRef && this.$refs.totalFromRef.resetFields()
      this.$refs.customSinkFormRef && this.$refs.customSinkFormRef.resetFields()
      this.customSinkTypes.forEach(type => {
        this.$refs[`${type}FormRef`] && this.$refs[`${type}FormRef`].resetFields()
      })

    },
    async buildData() {
      let name = this.$route.query.name

      if (!name) {
        return;
      }

      const res = await this.$Server('/task/copy?taskName=' + name, 'GET');
      let dataResultObject = res.data.resultObject;
      this.taskFrom.type = dataResultObject.drcTaskVO.type
      this.taskFrom.taskName = dataResultObject.drcTaskVO.taskName
      this.taskFrom.desc = dataResultObject.drcTaskVO.desc
      this.taskFrom.qpsLimitConfig = dataResultObject.drcTaskVO.qpsLimitConfig

      if(dataResultObject.drcSubTaskIncrVO !== null) {
        console.log("dataResultObject.drcSubTaskIncrVO !== null");
        this.taskFrom.supportDDLSync = dataResultObject.drcSubTaskIncrVO.supportDDLSync
        this.taskFrom.DDLSyncFilterDML = dataResultObject.drcSubTaskIncrVO.DDLSyncFilterDML
      }else{
        console.log("dataResultObject.drcSubTaskIncrVO === null");
      }

      if (dataResultObject.drcTaskVO.sinkType !== 0) {
        this.customSinkForm.useCustomSink = true
        this.taskFrom.sinkType = dataResultObject.drcTaskVO.sinkType
        if (this.taskFrom.sinkType === 1) {
          this.customSinkForm.customSinkType = "Kafka"
          this.KafkaForm = dataResultObject.drcTaskVO.kafkaSinkConfig
        }
        if (this.taskFrom.sinkType === 2) {
          this.customSinkForm.customSinkType = "MySQL"
          this.MySQLForm = dataResultObject.drcTaskVO.mySQLSinkConfig
        }
        if (this.taskFrom.sinkType === 3) {
          this.customSinkForm.customSinkType = "RocketMQ"
          this.RocketMQForm = dataResultObject.drcTaskVO.rocketSinkConfig
        }
        if (this.taskFrom.sinkType === 4) {
          this.customSinkForm.customSinkType = "CanalKafka"
          this.CanalKafkaForm = dataResultObject.drcTaskVO.canalKafkaSinkConfig
        }
      }

      // 过滤表达式
      this.taskFrom.tableExpression = !res.data.resultObject.drcSubTaskIncrVO ? "" : res.data.resultObject.drcSubTaskIncrVO.tableExpression
      this.taskFrom.tableName = !res.data.resultObject.fullTaskConfigVO ? "" : res.data.resultObject.fullTaskConfigVO.tableName;
      this.taskFrom.rangeSizeConfig = !res.data.resultObject.fullTaskConfigVO ? "" : res.data.resultObject.fullTaskConfigVO.rangeSizeConfig;
      this.taskFrom.whereStatement = !res.data.resultObject.fullTaskConfigVO ? "" : res.data.resultObject.fullTaskConfigVO.whereStatement;
      this.taskFrom.selectFieldList = !res.data.resultObject.fullTaskConfigVO ? "" : res.data.resultObject.fullTaskConfigVO.selectFieldList;
      this.taskFrom.kafkaSinkConfig = res.data.resultObject.kafkaSinkConfig
      this.taskFrom.mySQLSinkConfig = res.data.resultObject.mySQLSinkConfig
      this.taskFrom.rocketSinkConfig = res.data.resultObject.rocketSinkConfig

      this.totalFrom.url = !res.data.resultObject.fullDbConfig ? "" : res.data.resultObject.fullDbConfig.url
      this.totalFrom.username = !res.data.resultObject.fullDbConfig ? "" : res.data.resultObject.fullDbConfig.username
      this.totalFrom.pwd = !res.data.resultObject.fullDbConfig ? "" : res.data.resultObject.fullDbConfig.pwd
      this.totalFrom.database = !res.data.resultObject.fullDbConfig ? "" : res.data.resultObject.fullDbConfig.database

      this.incrFrom.url = !res.data.resultObject.incrDbConfig ? "" : res.data.resultObject.incrDbConfig.url
      this.incrFrom.username = !res.data.resultObject.incrDbConfig ? "" : res.data.resultObject.incrDbConfig.username
      this.incrFrom.pwd = !res.data.resultObject.incrDbConfig ? "" : res.data.resultObject.incrDbConfig.pwd
      this.incrFrom.database = !res.data.resultObject.incrDbConfig ? "" : res.data.resultObject.incrDbConfig.database
    }
  }
}
</script>

<style scoped lang="less">
@import "../../assets/style/var";

.add {
  padding: 20px;
  background: @--background-color-base;
}

.add-box {
  height: 100%;
  //background: @--background-color-base;
  overflow-y: scroll;

  .card {
    margin-bottom: 0px;
  }

  &__tip {
    padding-left: 5px;
    border-radius: 4px;
    font-size: 12px;
    color: #e6a23c;
  }

  .el-form {
    width: 800px;
  }

  /deep/ .el-form-item__label {
    font-size: 14px;
    padding-right: 10px;
  }
}

.is-card-last {
  //padding-bottom: 0px;
  margin-bottom: 0px;
}

.config-list {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  color: #4d4d4d;

  .config-item {
    margin-top: 8px;
    margin-right: 10px;
    cursor: pointer;
    display: flex;
    align-items: center;
    padding-left: 6px;
    text-align: center;
    font-size: 14px;
    line-height: 24px;
    height: 24px;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    background-color: rgba(233, 233, 233, 1);

    &:hover {
    }

    .el-icon-close {
      padding: 5px;

      &:hover {
        color: #0975e1;
      }
    }
  }
}

.btn-box {
  width: 100%;
  margin-top: 30px;
  padding-left: 50px;
  height: 50px;
}

/deep/ .el-input, .el-textarea {
  width: 380px;
}

.el-form-item {
  margin-bottom: 4px;
}

</style>
