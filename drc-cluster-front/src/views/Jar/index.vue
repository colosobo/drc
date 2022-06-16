<template>
  <PageContent>
    <div class="jar-box">
      <Card style="background: white" title="Jar包任务">
        <el-form ref="form" :model="jarForm" label-width="100px">
          <el-form-item label="Jar包">
            <el-upload class="upload-demo" action="" :http-request="uploadJar" :file-list="fileList" :limit="1" list-type="picture-card"
                       :on-exceed="handleExceed">
              <el-button type="primary" size="small">选择 Jar 文件</el-button>
              <div slot="tip" class="el-upload__tip"></div>
            </el-upload>
            <el-button type="success" size="small" style="width: 150px">检查jar是否合法</el-button>
          </el-form-item>
          <el-form-item label="任务Key">
            <el-input v-model="jarForm.key"></el-input>
          </el-form-item>
          <el-form-item label="任务描述">
            <el-input v-model="jarForm.desc"></el-input>
          </el-form-item>
          <el-form-item label="设定运行时长">
            <el-input type="number" v-model="jarForm.duration"></el-input> 秒
            <p class="tips">tips:如果不设置，就一直运行，可手动停止。如果设置了，当运行到指定时间期限后，就会自动关闭。</p>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small">立即创建</el-button>
            <el-button type="info" @click="lookCode" size="small">查看示例代码</el-button>
            <span style="color: darkred ; font-size: 9px; "> * 注意: Maven 打包时,需要将你的依赖一起打包进去, 否则,可能会出现找不到依赖的情况.</span>
          </el-form-item>
        </el-form>
      </Card>
    </div>
    <div v-highlight>
      <el-dialog v-highlight title="示例代码"
                 :visible.sync="dialogVisible" width="70%"><pre class="language-java"><code v-highlight>
{{jarTaskCode}}
 </code></pre>
        <el-button type="primary" @click="copyText" class="copy-btn">
          复制代码
        </el-button>
      </el-dialog>
    </div>
  </PageContent>
</template>

<script>
import Card from '@/components/Card'
export default {
  components: {
    Card,
  },
  data() {
    return {
      jarForm: {
        desc: '',
        key: '',
        duration: '',
      },
      formData: null,
      fileList: [],
      dialogVisible: false,
      jarTaskCode: `public BaseResult addIncrTask(@RequestBody IncrTaskInput task) {
        taskNameValidator.valid(task.getDrcTaskVO().getTaskName(), true);
         dbConfigValidator.valid(task.getDbConfigVO().getUrl());
         int sinkType = task.getDrcTaskVO().getSinkType();
         if (sinkType == SinkConfig.Type.MYSQL.getCode()) {
         if (task.getDrcSubTaskIncrVO().getTableExpression().contains(",")) {
            throw new RuntimeException("当选择 MySQL 作为 Sink 时, 暂时不支持多表同步.");
          }
         if (task.getDrcSubTaskIncrVO().getTableExpression().endsWith("*")) {
            throw new RuntimeException("当选择 MySQL 作为 Sink 时, 暂时不支持多表同步.");
           }
        }
        taskService.addIncrTask(task);
         return new BaseResult();
    }`,
    }
  },
  methods: {
    uploadJar(param) {
      const fileObject = param.file
      this.formData = new FormData()
      this.formData.append('file', fileObject)
    },
    lookCode() {
      this.dialogVisible = true
    },
    copyText() {
      const input = document.createElement('input')
      document.body.appendChild(input)
      input.setAttribute('value', this.jarTaskCode)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success('复制成功')
    },
    handleExceed() {
      this.$message.warning(`当前限制选择 1 个文件`)
    },
  },
}
</script>
<style lang="less" scoped>
.jar-box {
  height: 100%;
  overflow: auto;

  .tips {
    color: #f56c6c;
    font-size: 14px;
    line-height: 15px;
  }
}

.copy-btn {
  margin-top: 20px;
  padding: 10px 15px;
}

.el-input {
  width: 400px;
  font-size: 14px;
}

.el-form-item {
  font-size: 14px;
}
</style>
