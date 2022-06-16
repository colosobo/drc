<template>
  <div class="pagination-box" v-if="total > pageSize">
    <el-pagination
      class="pagination f-right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page.sync="internalCurrentPage"
      layout="total, prev, pager, next, jumper"
      :page-sizes="pageSizes"
      :page-size="pageSize"
      :total="total"
    ></el-pagination>
  </div>
</template>

<script>
export default {
  props: {
    total: {
      type: Number,
      default: 1,
    },
    currentPage: {
      type: Number,
      default: 1,
    },
    pageSize: {
      type: Number,
      default: 10,
    },
    pageSizes: {
      type: Array,
      default: () => [20, 30, 50, 100],
    },
  },
  name: 'Pagination',
  data() {
    return {
      internalCurrentPage: 1,
    }
  },
  computed: {
    // internalCurrentPage() {
    //   return this.currentPage
    // }
  },
  watch: {
    currentPage: {
      immediate: true,
      handler(newVal) {
        this.internalCurrentPage = newVal
      }
    },
  },
  methods: {
    handleSizeChange() {
      this.$emit('size-change', ...arguments)
    },
    handleCurrentChange(newVal) {
      this.$emit('update:currentPage', newVal)
      this.$emit('current-change', ...arguments)
    },
  },
}
</script>

<style scoped lang="less">
.pagination-box {
  margin-top: 2px;
  height: 52px;
  width: 100%;
  box-shadow: 0 0px 10px rgba(0, 0, 0, 0.12);
  z-index: 1000;

  .pagination {
    margin-top: 20px;
  }
}
</style>
