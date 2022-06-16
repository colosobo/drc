<template>
  <div>

    <PageContent>
<!--      <div slot="form" class="page-form">-->
<!--        <form-item label="子任务状态">-->
<!--          <el-input @keyup.enter.native="getTableData" v-model="stateFilter"-->
<!--                    placeholder="请输入Key"></el-input>-->
<!--        </form-item>-->
<!--        <div class="btn-box">-->
<!--          <el-button size="small" type="primary" @click="getTableData">查询</el-button>-->
<!--          <el-button size="small" @click="resetData">重置</el-button>-->
<!--        </div>-->
<!--      </div>-->

      <div class="page-main">
        <ListLayout>
          <el-table slot="list" :data="tableData" width="100%" height="100%" :border="true">
            <el-table-column label="任务名称" prop="taskName"></el-table-column>
            <el-table-column label="任务类型" prop="typeText"></el-table-column>
            <!--            <el-table-column label="当前QPS" prop="currentQPS"></el-table-column>-->
            <el-table-column label="状态" prop="stateText"></el-table-column>
            <el-table-column label="拓展字段配置" prop="extText"></el-table-column>

            <el-table-column label="操作" prop="test">
              <div slot-scope="scope">
                <el-switch
                  v-model="scope.row.switchValue"
                  @change="(val) => tatusChange(val, scope.row)"
                  active-text="启用"
                  :disabled="switchDisabled"
                  inactive-text="停用"
                  active-color="#2981D9"
                  inactive-color="#CBCBCB"></el-switch>
              </div>

            </el-table-column>
            <el-table-column label="详情">
              <div slot-scope="scope">
                <el-button type="text" @click="showSubTask(scope.row)">详情</el-button>
                <el-button type="text" @click="showLog(scope.row)">查看日志</el-button>
              </div>
            </el-table-column>
            <div class="table-empty" slot="empty" style="width: 100%;">
              <img src="@/assets/images/empty.png" alt="" width="305" height="159"/>
            </div>
          </el-table>
          <Pagination
            slot="pagination"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :total="totalCount"
            :current-page.sync="currentPage">
          </Pagination>
        </ListLayout>
      </div>
    </PageContent>


    <el-dialog
      title="查看日志(当前自动刷新)"
      fullscreen="true"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="beforeClose"
      :visible.sync="logVisible">

      <pre>{{ logText }}</pre>

      <div class="footer-box" slot="footer">
        <el-button size="large" type="primary" @click="hide()">关闭</el-button>
      </div>

    </el-dialog>

    <el-dialog
      title="任务分片详情"
      width="480px"
      custom-class="dialog-reset"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      :before-close="beforeClose"
      :visible.sync="visible">
      <el-form ref="qpsSetFromRef"
               class="created-from"
               label-width="120px">
        <el-form-item label="当前的 QPS 配置：" v-if="subTaskInfo.type == 2">
          <span>{{ subTaskInfo.qpsConfig }}</span>
        </el-form-item>
        <el-form-item label="分片实际大小：" v-if="subTaskInfo.rangeSize">
          <span>{{ subTaskInfo.rangeSize }}</span>
        </el-form-item>
        <el-form-item label="分片最大主键：" v-if="subTaskInfo.sliceMaxPk">
          <span>{{ subTaskInfo.sliceMaxPk }}</span>
        </el-form-item>
        <el-form-item label="分片最小主键：" v-if="subTaskInfo.sliceMinPk">
          <span>{{ subTaskInfo.sliceMinPk }}</span>
        </el-form-item>
        <el-form-item label="完成的行数：" v-if="subTaskInfo.finishRowCount">
          <span>{{ subTaskInfo.finishRowCount }}</span>
        </el-form-item>
        <el-form-item label="当前扫描到的主键：" v-if="subTaskInfo.cursor">
          <span>{{ subTaskInfo.cursor }}</span>
        </el-form-item>
        <el-form-item label="状态：" v-if="subTaskInfo.state">
          <span>{{ subTaskInfo.stateText }}</span>
        </el-form-item>
        <el-form-item label="表名：" v-if="subTaskInfo.tableName">
          <span>{{ subTaskInfo.tableName }}</span>
        </el-form-item>
        <el-form-item label="拆分的分片总数：">
          <span>{{ subTaskInfo.sliceCount }}</span>
        </el-form-item>
        <el-form-item label="已经 select 完的分片数量：" v-if="subTaskInfo.finishSliceCount">
          <span>{{ subTaskInfo.finishSliceCount }}</span>
        </el-form-item>
        <el-form-item label="过滤表达式：" v-if="subTaskInfo.tableExpression">
          <span>{{ subTaskInfo.tableExpression }}</span>
        </el-form-item>
      </el-form>
      <div class="footer-box" slot="footer">
        <el-button size="small" type="primary" @click="hide">确定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
export default {
  name: 'SubTask',
  data() {
    return {
      autoRereshMap: {},
      options: [
        {value: 1, label: '增量',},
        {value: 2, label: '全量',},
        {value: 3, label: '混合',}
      ],
      stateMap: {
        0: '初始化结束',
        1: '运行中',
        2: '正常结束',
        3: '手动停止',
        4: '任务异常停止',
        8: '暂存中',
        9: '回放中',
        10: 'db是运行时,但 RPC 调用异常'
      },
      // 列表
      tableData: [],
      totalCount: 0,
      currentPage: 1,
      pageSize: 10,
      loading: false,
      switchDisabled: false,
      visible: false,
      logVisible: false,
      logText: "1111",
      rangeSize: 0,
      logObject: {},
      subTaskInfo: {
        qpsConfig: '34242342',
      },
    }
  },
  created() {
    this.getTableData()
    //setInterval(this.getTableData,1000)
  },
  methods: {
    showSubTask(row) {
      console.log('row', row)
      this.subTaskInfo = row
      this.visible = true
    },

    async doRefreshLog() {
      setInterval(this.getWokerList, 1000)
      const res = await this.$Server('task/getLog/' + this.logObject.taskName + "/" + 20, 'DELETE');
      this.logText = res.data.resultObject;
    },
    async showLog(row) {
      console.log('row', row)
      this.logObject = row;
      const res = await this.$Server('task/getLog/' + row.taskName + "/" + 20, 'DELETE');
      this.logText = res.data.resultObject;
      // eslint-disable-next-line no-prototype-builtins
      if (!(this.autoRereshMap.hasOwnProperty(row.taskName))) {
        let t = setInterval(this.doRefreshLog, 1000);
        this.autoRereshMap[row.taskName] = t;
      }

      this.logVisible = true
    },
    hide() {
      this.visible = false
      this.logVisible = false;
      for (var key in this.autoRereshMap) {
        console.log("key " + key)
        let t = this.autoRereshMap[key];
        clearInterval(t);
      }
      this.autoRereshMap = {}
    },
    beforeClose() {
      this.hide()
    },
    async getTableData() {

      const {taskId} = this.$route.params
      if (taskId == null) {
        return;
      }

      const res = await this.$Server('/task/subList', 'GET', {
        page_num: this.currentPage,
        page_size: this.pageSize,
        parentId: taskId
      });

      const {resultObject} = res.data
      this.tableData = resultObject.map(item => {
        return {
          ...item,
          typeText: this.options.find(o => o.value === item.type).label,
          stateText: this.stateMap[item.state],
          switchValue: item.switchState === 1,
          extText: JSON.stringify(item.ext)
        }
      })

      console.log(res)
    },
    async tatusChange(val, row) {
      console.log('val, row', val, row.taskName)
      const taskName = row.taskName
      try {
        if (this.switchDisabled) return

        this.switchDisabled = true

        if (val === true) {
          await this.$Server(`/task/start/${taskName}`, 'PUT', {})
        }

        if (val === false) {
          await this.$Server(`/task/stop/${taskName}`, 'PUT', {})
        }

      } catch (e) {
        console.log(e)
      } finally {
        this.switchDisabled = false
        // 页面不刷新.
        // this.getTableData()
      }

    },
    handleClickBtn() {

    },
    handleSizeChange() {
      this.getTableData()
    },
    handleCurrentChange() {
      this.getTableData()
    },
  }
}
</script>

<style scoped lang="less">
.el-form-item {
  width: 100%;
  margin-bottom: 8px;
  border-bottom: 1px solid #cccccc;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
