<template>

  <div class="index-page" style="overflow:auto">
    <Card title="QPS Summary">
      <div>
        <div class="task-review">
          <div class="task-review-item">
            <el-button type="success" class="auto-class" v-if="clusterState == '集群正常'" size="small">
              集群状态正常
            </el-button>

            <el-button type="warning" class="auto-class" v-if="clusterState != '集群正常'" size="small">
              {{ clusterState }}
            </el-button>

            <el-button type="info" class="auto-class" size="small"
                       style="background: url(../../assets/容器@2x.png)">
              Worker 节点数量 : {{ wokerList.length }}
            </el-button>

            <el-button type="info" class="auto-class" size="small"
                       style="background: url(../../assets/容器@2x.png)">
              Task 总数 : {{ count }}
            </el-button>

            <el-button type="info" class="auto-class" size="small"
                       style="background: url(../../assets/容器@2x.png)">
              集群总 QPS : {{ clusterQPS }}
            </el-button>

            <el-button type="info" class="auto-class" size="small"
                       style="background: url(../../assets/容器@2x.png)">
              全量 Task 总数：{{ fullCount }}
            </el-button>

            <el-button type="info" class="auto-class" size="small"
                       style="background: url(../../assets/容器@2x.png)">
              增量 Task 总数：{{ incrCount }}
            </el-button>

            <LineChart :dataSource="QPSData" :coordinateTransformMap="coordinateTransformMap"
                       ref="lineChartRef"/>
          </div>
        </div>
      </div>
    </Card>


    <Card title="Running Worker List">
      <div class="my-worker-list">
        <div style="position: relative; width: 350px;"   v-for="(work, workIndex) in wokerList" :key="workIndex">
          <span class="my-worker-item-name" style="position: absolute ;">{{ work.name +  work.workerQPS }}</span>
          <br/>
<!--          <img src="../../assets/服务器.png" style="width:350px;"-->
          <img src="../../assets/gif/222.gif" style="width:350px;"
               v-on:click="taskItemDialogClick(work.taskNameList)">
<!--          <img src="../../assets/worker.png" style="width:250px;"-->
        </div>
      </div>
    </Card>


    <Card title="Task Overview">
      <div style="background: black" class="my-icon">
        <div style="position: relative; width: 250px;" class="my-icon-item"
             align="center" v-for="(task, taskIndex) in taskList" :key="taskIndex" v-if="taskIndex < 8">
          <span v-if="taskIndex < 8" class="myTaskName" style="position: absolute; ;">{{ task.name}}</span><br>
<!--          <img v-if="taskIndex < 8" src="../../assets/icon.png" style="width:250px;">-->
          <img v-if="taskIndex < 8" src="../../assets/gif/111.gif" style="width:250px;">
        </div>
      </div>
    </Card>

    <Card title="集群操作">
      <el-form label-position="top" class="index-form" ref="indexFormRef"
               :model="indexForm"
               :rules="indexFormRules">
        <el-form-item label="自动 ReBalance">
          <el-button type="danger" class="auto-class" size="small" @click="autoRebalance">
            AutoReBalance
          </el-button>
          <span style="color: coral; font-size: 12px">注意：此按钮会触发 Worker 集群所有 Task 的重新散列，请谨慎操作!</span>
        </el-form-item>
        <el-form-item label="手动转移任务到新节点" style="color: #928020; font-size: 19px">
          将
          <el-form-item label="" prop="taskName" style="display: inline-block">
            <el-input v-model="indexForm.taskName" placeholder="请输入 task 节点"
                      class="task-input"></el-input>
          </el-form-item>
          转移到
          <el-form-item label="" prop="workerTcpUrl" style="display: inline-block">
            <el-select v-model="indexForm.workerTcpUrl" placeholder="请选择">
              <el-option
                v-for="(item, key) in options"
                :key="key"
                :label="item.label"
                :value="item.value">
              </el-option>
            </el-select>
          </el-form-item>
          <el-button type="primary" class="transfer-btn" size="small" @click="failover">确定转移
          </el-button>
        </el-form-item>
      </el-form>
    </Card>

<!--    <Card title="Kafka 统计">-->
<!--      <div class="task-review">-->
<!--        <div class="task-review-item">数据总条数：{{ kafkaCount }}</div>-->
<!--        <div class="task-review-item">Topic 数量：{{ topicCount }}</div>-->
<!--      </div>-->
<!--    </Card>-->

    <el-dialog
      title="任务列表"
      :visible.sync="taskItemDialog"
      width="30%">
      <div class="task-list" v-for="task in taskItemDialogData">
        <div class="task-item" align="left">
          {{ task.name }}
        </div>
      </div>
    </el-dialog>

    <Card title="数据流向图">
      <img src="../../assets/gif/333.gif" style="text-align: center; width: 70%">
    </Card>

    <!--    <div id="mountNode"></div>-->

  </div>
</template>

<script>
  import Card from '@/components/Card'
  import LineChart from '@/components/LineChart'
  import G6 from '@antv/g6';

  export default {
    name: 'Index',
    components: {Card, LineChart},
    data() {

      const validateTaskName = (rule, value, callback) => {
        if (!value) {
          return callback(new Error('请输入 task 节点'))
        }

        callback()
      }
      const validateWorkerTcpUrl = (rule, value, callback) => {
        if (!value) {
          return callback(new Error('请选择节点'))
        }

        callback()
      }

      return {
        taskItemDialog: false,
        taskItemDialogData: "",
        options: [],
        indexFormRules: {
          taskName: {validator: validateTaskName, trigger: 'blur'},
          workerTcpUrl: {validator: validateWorkerTcpUrl, trigger: 'change'},
        },
        graph: null,
        mountNodeData: {
          // 点集
          nodes: [
            {
              id: 'node1', // String，该节点存在则必须，节点的唯一标识
              x: 100, // Number，可选，节点位置的 x 值
              y: 200, // Number，可选，节点位置的 y 值
            },
            {
              id: 'node2', // String，该节点存在则必须，节点的唯一标识
              x: 300, // Number，可选，节点位置的 x 值
              y: 200, // Number，可选，节点位置的 y 值
            },
          ],
          // 边集
          edges: [
            {
              source: 'node1', // String，必须，起始点 id
              target: 'node2', // String，必须，目标点 id
            },
          ],
        },
        indexForm: {
          taskName: '',
          workerTcpUrl: '',
        },
        wokerList: [],
        taskList: [],
        count: 0,
        clusterState: '正常',
        clusterQPS: '',
        workerCount: 0,
        fullCount: 0,
        incrCount: 0,
        loading: false,
        kafkaCount: 0,
        topicCount: '',
        QPSData: [],
        coordinateTransformMap: {
          x: 'timeInSeconds',
          y: 'qps'
        }
      }
    },
    created() {
      this.getWokerList()
      this.getClusterState()
      this.getRunningTaskCount()
      //this.getKafkaState()
      this.getClusterStateTimer = setInterval(this.getClusterState, 3000)
      this.getWokerListTimer = setInterval(this.getWokerList, 4000)
      this.getRunningTaskCountTimer = setInterval(this.getRunningTaskCount, 8000)
    },
    mounted() {

      this.init()
      this.graph = new G6.Graph({
        container: 'mountNode', // String | HTMLElement，必须，在 Step 1 中创建的容器 id 或容器本身
        width: 800, // Number，必须，图的宽度
        height: 500, // Number，必须，图的高度
      });
      this.graph.data(this.mountNodeData); // 读取 Step 2 中的数据源到图上
      this.graph.render(); // 渲染图
      G6.registerEdge(
        'circle-running',
        {
          afterDraw(cfg, group) {
            // get the first shape in the group, it is the edge's path here=
            const shape = group.get('children')[0];
            // the start position of the edge's path
            const startPoint = shape.getPoint(0);

            // add red circle shape
            const circle = group.addShape('circle', {
              attrs: {
                x: startPoint.x,
                y: startPoint.y,
                fill: '#1890ff',
                r: 3,
              },
              name: 'circle-shape',
            });

            // animation for the red circle
            circle.animate(
              (ratio) => {
                // the operations in each frame. Ratio ranges from 0 to 1 indicating the prograss of the animation. Returns the modified configurations
                // get the position on the edge according to the ratio
                const tmpPoint = shape.getPoint(ratio);
                // returns the modified configurations here, x and y here
                return {
                  x: tmpPoint.x,
                  y: tmpPoint.y,
                };
              },
              {
                repeat: true, // Whether executes the animation repeatly
                duration: 3000, // the duration for executing once
              },
            );
          },
        },
        'cubic', // extend the built-in edge 'cubic'
      );

      const data = {
        nodes: [
          {
            id: 'node1',
            // type: 'image',
            x: 100,
            y: 100,
            label: 'Node 1',
            labelCfg: {
              position: 'top',
            },
          },
          {
            id: 'node2',
            x: 300,
            y: 100,
            type: 'image',
            img: '../assets/容器@2x.png',
            color: '#40a9ff',
            label: 'Node 2',
            labelCfg: {
              position: 'left',
              offset: 10,
            },
          },
          {
            id: 'node3',
            x: 500,
            y: 100,
            label: 'Node 3',
            labelCfg: {
              position: 'top',
            },
          },
        ],
        edges: [
          {
            source: 'node1',
            target: 'node2',
          },
          {
            source: 'node2',
            target: 'node3',
          },
        ],
      };

      const container = document.getElementById('mountNode');
      const width = container.scrollWidth;
      const height = container.scrollHeight || 500;
      const graph = new G6.Graph({
        container: 'mountNode',
        width,
        height,
        defaultEdge: {
          type: 'circle-running',
          style: {
            lineWidth: 2,
            stroke: '#bae7ff',
          },
        },
      });
      graph.data(data);
      graph.render();

      if (typeof window !== 'undefined')
        window.onresize = () => {
          if (!graph || graph.get('destroyed')) return;
          if (!container || !container.scrollWidth || !container.scrollHeight) return;
          graph.changeSize(container.scrollWidth, container.scrollHeight);
        };

    },
    beforeDestroy() {
      clearInterval(this.timer)
      clearInterval(this.getClusterStateTimer)
      clearInterval(this.getWokerListTimer)
      clearInterval(this.getRunningTaskCountTimer)
    },
    methods: {
      async taskItemDialogClick(row) {
        this.taskItemDialog = true;
        // row.for
        console.log(row)
        this.taskItemDialogData = row
      },
      async init() {
        var e = parseInt(new Date().getTime() / 1000);
        var s = parseInt(new Date().getTime() / 1000) - 600;
        const res = await this.$Server('/home/qpsChart/' + s + "/" + e, 'GET', {})
        res.data.resultObject.map(item => {
          let i = {"timeInSeconds": item.timeInSeconds * 1000, "qps": item.qps}
          this.QPSData.push(i)
        })
        this.$refs.lineChartRef.initChart()

        this.timer = setInterval(() => {
          this.getQps()
        }, 1000);
      },

      async getQps() {
        var end = parseInt(new Date().getTime() / 1000);
        var start = parseInt(new Date().getTime() / 1000) - 20;
        const res = await this.$Server('/home/qpsChart/' + start + "/" + end, 'GET', {})
        var qps = res.data.resultObject[res.data.resultObject.length - 1].qps;
        var timeInSeconds = res.data.resultObject[res.data.resultObject.length - 1].timeInSeconds * 1000;
        this.$refs.lineChartRef.addPoint({timeInSeconds, qps});
      },

      async getClusterState() {
        const res = await this.$Server('/home/clusterSummary', 'GET', {})
        this.clusterState = res.data.resultObject.workerClusterState
        this.clusterQPS = res.data.resultObject.clusterTotalQPS
        this.workerCount = res.data.resultObject.workerCount
      },
      async getWokerList() {
        const res = await this.$Server('/home/list', 'GET', {})


        const {resultObject} = res.data

        this.wokerList = resultObject
        var taskArray = [];
        this.wokerList.map(worker => {
          worker.taskNameList.map(taskName => {
            taskArray.push(taskName)
          })
        })
        this.taskList = taskArray;
        this.options = []
        this.wokerList = resultObject.map(item => {
          this.options.push({
            value: item.ipPort,
            label: item.name,
          })

          return {
            ...item,
            workName: `${item.name}` + 'QPS:' + item.workerQPS
          }
        })
      },
      async getRunningTaskCount() {
        const res = await this.$Server('/home/runningTaskCount', 'GET', {})

        const {resultObject} = res.data

        this.count = resultObject.count || 0
        this.fullCount = resultObject.fullCount || 0
        this.incrCount = resultObject.incrCount || 0

      },

      async getKafkaState() {
        const res = await this.$Server('/home/kafkaStat', 'GET', {})

        const {resultObject} = res.data

        this.kafkaCount = resultObject.kafkaCount || 0
        this.topicCount = resultObject.topicCount || 0

      },


      async failover() {
        try {
          if (this.loading) return
          this.loading = true

          const flag = await this.$refs.indexFormRef.validate()
          console.log('flag', flag)

          await this.$confirm('是否确定操作', '提示', {
            type: 'warning',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            closeOnClickModal: false,
            closeOnPressEscape: false,
          })

          const res = await this.$Server('/home/failover', 'GET', {
            ...this.indexForm
          })

          console.log('res', res)

          this.$message.success('转移成功')

        } catch (e) {
          console.log(e)
        } finally {
          this.loading = false
        }
      },
      async autoRebalance() {
        try {
          await this.$confirm('是否确定操作', '提示', {
            type: 'warning',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            closeOnClickModal: false,
            closeOnPressEscape: false,
          })

          await this.$Server('/home/autoReBalance', 'GET', {})

          this.$message.success('操作成功')

        } catch (e) {
          console.log(e)
        } finally {
          console.log('finally')
        }
      }
    }
  }
</script>

<style lang="less">
.index-page {
  padding: 20px;
  background: #000000;
  margin: 0 auto;
  .el-dialog__header {
    background: #313335;
  }

  /* 弹出层设置背景色 底部*/

  .el-dialog__body {
    background-color: #313335;
  }

  .index-form {
    font-size: 14px;

    .el-form-item__label {
      font-size: 16px;
    }


    .task-input {
      width: 200px;
    }

  .auto-class {
    margin-right: 30px;
  }

    .transfer-btn {
      margin-left: 30px;
      background: #324dbf;
    }
  }

  .card + .card {
    margin-bottom: 20px;
  }

  .task-review {
    display: flex;
    //background: #000000;

    .task-review-item {
      width: 100%;
    }
  }


  .taskItemDialog {
    color: #0cfff8;
    margin: 1% 1% 1% 1%;
  }


  .my-icon {
    display: flex;
    flex-flow: row wrap;

    .my-icon-item {
      margin: 1% 1% 1% 1%;
      /*text-align: center;*/
      border-radius: 20px;
      &:hover {
        box-shadow: 0 0 8px 0 rgb(232 237 250 / 90%), 0 5px 8px 0 rgb(232 237 250 / 90%);
      }
      .myTaskName {
        font-size: 10px;
        text-align: center;
        top: 8px;
        left: 60px;
        color: #0e3e49;
      }
    }
  }

  .my-worker-list {
    display: flex;
    flex-flow: row wrap;

    img {
      border-radius: 20px;
      margin: 0% 7% 0% 7%;

      &:hover {
        box-shadow: 0 0 8px 0 rgb(232 237 250 / 90%), 0 5px 8px 0 rgb(232 237 250 / 90%);
      }
    }


    .my-worker-item-name {
      font-size: 10px;
      top: 1px;
      left:115px;
      color: #1662bd;
    }

  }

  .task-list {

    .task-item {
      margin: 2% 2% 2% 2%;
      color: #4a90e2;
      font-size: 13px;
      text-align: left;
    }
  }
}
</style>
