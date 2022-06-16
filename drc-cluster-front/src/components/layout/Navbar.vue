<template>
  <div class="navbar">
<!--    <div class="header f-clear">-->
<!--      &lt;!&ndash;<div class="go-datav f-right" @click="goDatav"><i class="esign-icon-kongzhitai"></i>进入大屏</div>&ndash;&gt;-->
<!--    </div>-->
    <Breadcrumb></Breadcrumb>
  </div>
</template>

<script>
import Breadcrumb from '@/components/Breadcrumb'

export default {
  name: 'Navbar',
  components: {
    Breadcrumb
  },
  methods: {
    goDatav() {
      this.$router.push({
        path: '/datav'
      })
    }
  }
}
</script>

<style scoped lang="less">
@import "../../assets/style/var";

.navbar {
  width: 100%;
  flex: 0 0 auto;

  .header {
    height: 57px;
    background: @--background-color-black;

    .go-datav {
      cursor: pointer;
      user-select: none;
      margin-right: 20px;
      width: 100px;
      height: 57px;
      line-height: 57px;
      text-align: center;
      font-size: 12px;
      color: #ffffff;

      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
        color: rgba(4, 86, 254, 1);
      }

      .esign-icon-kongzhitai {
        margin-right: 6px;
      }
    }
  }
}
</style>
