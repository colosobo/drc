<template>
  <PageContent>

    <div slot="form" class="page-form">
      <form-item label="任务Key">
        <el-input @keyup.enter.native="getTableData" v-model="taskName"
                  placeholder="请输入Key"></el-input>
      </form-item>
      <div class="btn-box">
        <el-button size="small" type="primary" @click="getTableData">查询</el-button>
        <el-button size="small" @click="resetData">重置</el-button>
      </div>
    </div>
    <div class="page-main">
      <ListLayout>
        <el-table slot="list" :data="tableData" width="100%" height="100%" :border="true">
          <el-table-column label="任务Key" prop="name"></el-table-column>
          <el-table-column label="QPS" prop="qps" align="left" width="89px"></el-table-column>
          <el-table-column label="任务类型" prop="typeText" align="left" width="80px"></el-table-column>
          <el-table-column label="任务描述" prop="desc"></el-table-column>
          <el-table-column label="Sink 目标" prop="destName" width="129px"></el-table-column>
          <el-table-column label="是否包含异常任务" width="150px">
            <div slot-scope="scope">
              <el-button type="danger" round v-if="scope.row.hasException==true">异常</el-button>
              <el-button type="success" round v-if="scope.row.hasNormal==true">正常</el-button>
            </div>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="175px"
                           align="left"></el-table-column>
          <!--          <el-table-column label="kafka总数" prop="kafkaTotal"></el-table-column>-->
          <!--          <el-table-column label="kafka更新时间" prop="kafkaUpdateTime" width="175px"></el-table-column>-->
          <el-table-column label="删除" width="79px">
            <div slot-scope="scope">
              <el-button type="text" @click="deleteParentTask(scope.row)">删除</el-button>
            </div>
          </el-table-column>
          <el-table-column label="QPS配置" width="120px">
            <div slot-scope="scope">
              <el-button type="text" @click="qpsSeting(scope.row)">配置
              </el-button>
            </div>
          </el-table-column>
          <el-table-column label="操作">
            <div slot-scope="scope">
<!--              <el-button type="text" v-if="scope.row.canSplit"-->
<!--                         @click="taskSplit(scope.row, false)">仅拆分-->
<!--              </el-button>-->

              <el-button type="text" v-if="scope.row.canSplit"
                         @click="taskSplit(scope.row, true)">拆分并启动
              </el-button>

              <span v-if="!scope.row.canSplit && scope.row.type != 1">无</span>
              <span v-if="scope.row.type == 1">无</span>
              <br/>
              <el-button type="text" @click="copyTask(scope.row)">复制</el-button>
              <el-button type="text" @click="showSubTask(scope.row)">查看子任务</el-button>
            </div>
          </el-table-column>
<!--          <el-table-column label="子任务" prop="test">-->
<!--            <div slot-scope="scope">-->
<!--              <el-button type="text" @click="showSubTask(scope.row)">查看子任务</el-button>-->
<!--&lt;!&ndash;              <el-button v-if="scope.row.type != 1" type="text" @click="startAllNotRunningFullTask(scope.row)">&ndash;&gt;-->
<!--&lt;!&ndash;                启动所有全量子任务&ndash;&gt;-->
<!--&lt;!&ndash;              </el-button>&ndash;&gt;-->

<!--              <br/>-->
<!--            </div>-->
<!--          </el-table-column>-->
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

      <el-dialog
        title="QPS配置"
        width="520px"
        custom-class="dialog-reset"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :before-close="beforeClose"
        :visible.sync="visible">

        <el-form ref="qpsSetFromRef" :model="qpsSetFrom"
                 class="created-from"
                 label-width="120px">
          <el-form-item label="全局 qps" prop="qps">
            <el-input v-model="qpsSetFrom.qps"></el-input>
          </el-form-item>

        </el-form>
        <div class="footer-box" slot="footer">
          <el-button size="small" @click="hide">取消</el-button>
          <el-button size="small" type="primary" @click="submit">确定</el-button>
        </div>
      </el-dialog>
    </div>

  </PageContent>
</template>

<script>
export default {
  name: 'Task',
  data() {
    return {
      time: 0,
      taskName: '',
      taskType: '',
      options: [
        {value: 1, label: '增量',},
        {value: 2, label: '全量',},
        {value: 3, label: '混合',},
        {value: 4, label: '库同步',}
      ],
      visible: false,
      qpsSetFrom: {
        qps: -1,
        taskName: ''
      },
      qpsSetFromRules: {
        // fullQps: {required: true, type: 'number', message: '请输入'}
        // incrQps: {required: true, type: 'number', message: '请输入', trigger: 'blur'},
      },
      currentQpsSet: {},
      splitStateOptions: [
        {value: 6, label: '拆分中',},
        {value: 7, label: '运行中',},
        {value: 0, label: '初始化结束',},
        {value: 1, label: '运行中',},
        {value: 3, label: '手动停止',},
        {value: 5, label: '准备删除',}
      ],
      // 列表
      tableData: [],
      totalCount: 0,
      currentPage: 1,
      pageSize: 10,
      loading: false,
    }
  },
  created() {
    this.getTableData()
    // this.getTableDataTimer = setInterval(this.getTableData, 2000)
  },
  beforeDestroy() {
    clearInterval(this.getTableDataTimer)
  },
  methods: {
    async getTableData() {
      this.time++
      const res = await this.$Server('/task/parentList', 'GET', {
        page_num: this.currentPage,
        page_size: this.pageSize,
        taskName: this.taskName
      })

      const {list, totalItems} = res.data.resultObject
      this.tableData = list.map(item => {
        return {
          ...item,
          typeText: this.options.find(o => o.value === item.type).label,
          // splitStateText: this.splitStateOptions.find(o => o.value === item.state).label
        }
      })

      this.totalCount = totalItems

      console.log(res)
    },
    async qpsSeting(row) {
      this.visible = true
      this.currentQpsSet = row
      const res = await this.$Server('/task/getQpsConfig/' + this.currentQpsSet.name, 'GET')
      console.log(res)
      this.qpsSetFrom.qps = (res.data.resultObject.qps * 1);
    },
    hide() {
      this.visible = false
    },
    async submit() {
      try {
        if (this.loading) return
        this.loading = true

        await this.$Server('/task/updateQpsConfig', 'POST', {
          ...this.qpsSetFrom,
          taskName: this.currentQpsSet.name
        })

        this.$message.success('设置成功')

        this.loading = false
        this.visible = false
        this.qpsSetFrom.qps = -1

        this.getTableData()

      } catch (e) {
        console.log(e)
      } finally {
        this.loading = false
      }

    },
    beforeClose() {
      this.hide()
      this.currentQpsSet = {}
    },
    resetData() {
    },

    async deleteParentTask(row) {

      const {id} = row

      await this.$confirm('删除会导致所有子任务停止并不可恢复, 是否确定删除', '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        closeOnPressEscape: true,
      })

      await this.$Server('/task/parent/' + id, 'DELETE', {})

      this.$message.success('删除成功')

      this.getTableData()
    },
    showSubTask(row) {
      const {id} = row

      this.$router.push({
        path: `task/${id}`
      })
    },
    copyTask(row) {
      console.log(row.name)
      this.$router.push({
        path: `add/?name=` + row.name
      })
    },
    async startAllNotRunningFullTask(row) {
      await this.$confirm('确定启动所有全量子任务吗? ', '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        closeOnPressEscape: true,
      })
      await this.$Server('task/startAllNotRunningFullTask/' + row.name, 'GET');
      this.$message.success('启动子任务成功')
    },
    async taskSplit(row, start) {
      console.log('row', row)
      try {
        if (this.loading) return

        this.loading = true

        const {id} = row

        if (start) {
          await this.$confirm('是否确定拆分并启动? 如果同步任务很大,启动大量任务,可能耗尽数据库连接', '提示', {
            type: 'warning',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            closeOnClickModal: false,
            closeOnPressEscape: false,
          });
        }

        var path = '/task/split/' + id + "/" + start;
        console.log(path)
        await this.$Server(path, 'POST', {})

        if (start) {
          this.$message.success('开始执行拆分并执行全量同步任务, 请注意观察 QPS 变化');
        } else {
          this.$message.success('执行拆分中.....');
        }

        this.loading = false;

        this.getTableData()

      } catch (e) {
        console.log(e)
      } finally {
        this.loading = false
      }
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

<style scoped>

</style>
