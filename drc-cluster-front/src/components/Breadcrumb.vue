<template>
<!--  <div class="breadcrumb-box">-->
<!--&lt;!&ndash;    <el-breadcrumb>&ndash;&gt;-->
<!--&lt;!&ndash;&lt;!&ndash;      <el-breadcrumb-item v-for="(item,index) in levelList" :key="item.path">&ndash;&gt;&ndash;&gt;-->
<!--&lt;!&ndash;&lt;!&ndash;        <span class="no-redirect" v-if="index === levelList.length-1">{{ item.meta.title }}</span>&ndash;&gt;&ndash;&gt;-->
<!--&lt;!&ndash;&lt;!&ndash;        <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>&ndash;&gt;&ndash;&gt;-->
<!--&lt;!&ndash;&lt;!&ndash;      </el-breadcrumb-item>&ndash;&gt;&ndash;&gt;-->
<!--&lt;!&ndash;    </el-breadcrumb>&ndash;&gt;-->
<!--  </div>-->
</template>

<script>
export default {
  name: 'Breadcrumb',
  data() {
    return {
      levelList: null
    }
  },
  watch: {
    $route(route) {
      // if you go to the redirect page, do not update the breadcrumbs
      // if (route.path.startsWith('/redirect/')) {
      //   return
      // }
      this.getBreadcrumb(route)
    }
  },
  created() {
    this.getBreadcrumb()
  },
  methods: {
    getBreadcrumb() {
      console.log('this.$route----', this.$route)
      let matched = this.$route.matched.filter(item => item.meta && item.meta.title)

      this.levelList = matched.filter(item => item.meta && item.meta.title)

      // console.log('levelList', this.levelList)
    },
    handleLink(item) {
      const { name } = item
      this.$router.push({
        name,
        params: this.$route.params
      })
    }
  }
}
</script>

<style scoped lang="less">
@import "../assets/style/var";

.breadcrumb-box {
  padding: 16px 20px 0px 20px;
  height: 36px;
  background: @--background-color-base;
}
</style>
