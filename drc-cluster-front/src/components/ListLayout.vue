<template>
  <div class="list-layout" :style="layoutStyle">
    <div class="list-body">
      <slot name="list"></slot>
    </div>
    <div class="pagination-body" v-if="$slots.pagination">
      <slot name="pagination"></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ListLayout',
  components: {},
  mixins: {},
  props: {
    height: [String, Number],
  },
  data() {
    return {}
  },
  computed: {
    layoutStyle() {
      return {
        height: this.height ? `${ this.height }px` : '100%',
      }
    },
  },
  watch: {},
  // beforeCreate() {
  // },
  created() {},
  // beforeMount() {
  // },
  mounted() {},
  methods: {},
}
</script>

<style lang="less">
.tooltip-popper-class {
  max-width: 260px;
  line-height: 24px;
}
</style>

<style lang="less" scoped>
.list-layout {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  background: #ffffff;
  overflow: hidden;

  .list-body {
    flex: 1;
    display: flex;
    overflow: hidden;

    /deep/ .el-table {
      border: none;

      &::before,
      &::after {
        background-color: transparent;
      }

      .el-table__fixed::before,
      .el-table__fixed-right::before {
        background-color: transparent;
      }

      th {
        padding: 0px;
        height: 48px;
        background: #f7f8fa;

        .cell {
          padding-left: 40px;
          font-size: 14px;
          color: #1c1c1c;
          background: transparent;
        }
      }

      td {
        padding: 0px;
        height: 48px;

        .cell {
          padding-left: 40px;
          font-size: 13px;
          color: rgba(0, 0, 0, 0.87);
        }
      }
    }
  }

  .pagination-body {
    flex: 0 0 auto;
  }
}
</style>
