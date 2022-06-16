<template>
  <div class="line-chart" ref="lineChartRef" style="width:100%;height:400px"></div>
</template>

<script>
import Highcharts from 'highcharts/highstock';
import HighchartsMore from 'highcharts/highcharts-more';
import HighchartsDrilldown from 'highcharts/modules/drilldown';
import Highcharts3D from 'highcharts/highcharts-3d';

HighchartsMore(Highcharts)
HighchartsDrilldown(Highcharts);
Highcharts3D(Highcharts);
export default {
  data() {
    return {
      chart: null,
    }
  },
  props: {
    dataSource: {
      type: Array,
      default() {
        return []
      }
    },
    coordinateTransformMap: {
      type: Object,
      default() {
        return null
      }
    },
    maxPoint: {
      type: Number,
      default: 500
    },
    title: {
      type: String,
      default: 'DRC 集群总QPS'
    }
  },
  methods: {
    transformCoordinateForList(origin) {
      if (!this.coordinateTransformMap) return origin
      if (Array.isArray(origin)) {
        const result = origin.map(item => {
          return this.coordinateFormat(item)
        })
        console.log(result)
        return result
      }
    },
    coordinateFormat(origin) {
      if (!this.coordinateTransformMap) return origin
      return {
        x: origin[this.coordinateTransformMap.x],
        y: origin[this.coordinateTransformMap.y],
      }
    },
    addPoint(point){
      const pointFomat = this.coordinateFormat(point)
      try{
        var series = this.chart.series[0],
        shift = series.data.length > this.maxPoint; // 当数据点数量超过 360 个，则指定删除第一个点
        series.addPoint([pointFomat.x, pointFomat.y], true, shift, true);
        this.activeLastPointToolip(this.chart)
      } catch(e){
        console.log(e)
      }
    },
    activeLastPointToolip(chart) {
      var points = chart.series[0].points;
      chart.tooltip.refresh(points[points.length -1]);
    },
    initChart() {
      var options = {
        chart: {
          shadow: true,
          backgroundColor: {
            linearGradient: [1, 1, 1, 500],
            stops: [
              [0,  '#090941'],
              [1,  '#02060c']
            ]
          },
        },
        credits:true,
        title: {
          text: this.title,
          style: {
            color: '#c6bbbb',
            margin: '20px',
            fontSize: '12px'
          },
        },
        xAxis: {
          type: 'datetime',
          tickPixelInterval: 100
        },
        yAxis: {
          title: {
            text: 'QPS',
          },
          gridLineWidth: 0,
          min : 0,
        },
        series: [{
          type: 'spline',
          animation: true,
          data: this.transformCoordinateForList(this.dataSource)
        }],
        legend: {
          enabled: false
        },
        tooltip: {
          formatter: function () {
            return `time: <b>${Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x)}</b>  <br/>QPS: <b>${Highcharts.numberFormat(this.y, 2) }</b><br/>`
          }
        },
      }
      this.chart = Highcharts.chart(this.$el, options);
    },

  },
  mounted() {
    Highcharts.setOptions({
      global: {
        useUTC: false
      }
    });
  }
}
</script>

<style lang="less">
.line-chart {
  margin-top: 20px;
  background: #05080c;
}
</style>
