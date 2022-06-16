<template>
  <div class="sidebar">
    <div class="menu">
      <div
        class="menu-item"
        :class="{ 'is-active': $route.path.startsWith(menuItem.path) }"
        v-for="(menuItem, mentIndex) in menus"
        :key="mentIndex"
        @click="changeRouter(menuItem)">
        <i :class="[menuItem.icon, 'menu-icon']"></i>
        <span>{{ menuItem.text }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'sidebar',
  data() {
    return {
      menus: [
        {
          text: '首页大盘',
          icon: 'esign-icon-icon_attacks',
          path: '/index',
        },
        {
          text: '任务列表',
          icon: 'esign-icon-icon_honeypotcluster',
          path: '/task',
        },
        {
          text: '新增',
          icon: 'esign-icon-icon_applications',
          path: '/add',
        },
        // {
        //   text: 'Jar包任务',
        //   icon: 'esign-icon-icon_densitys',
        //   path: '/jar-task',
        // },
      ],
    }
  },
  created() {
    console.log('$route.path', this.$route)
  },
  methods: {
    changeRouter(item) {
      const { path } = item
      this.$router.push({
        path: path,
      })
    },
  },
}
</script>

<style scoped lang="less">
@import '../../assets/style/var';

.sidebar {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  width: 180px;
  height: 100%;
  overflow: hidden;
  background: @--background-color-black;

  .logo {
    flex: 0 0 52px;
    width: 100%;
    height: 57px;
    background: url('../../assets/logo.png') no-repeat;
    background-size: 120px 32px;
    background-position: left 18px center;
  }

  .menu {
    flex: 1;
    overflow: hidden;

    .menu-item {
      cursor: pointer;
      padding: 0px 18px;
      height: 48px;
      line-height: 48px;
      color: #dddddd;
      font-size: 14px;
      font-weight: 400;
      transition: 0.15s ease-out;

      &:hover,
      &.is-active {
        color: #0cfff8;
        background: linear-gradient(270deg, rgba(1, 5, 9, 0) 0%, #105ae2 100%);
      }

      .menu-icon {
        margin-right: 11px;
      }
    }
  }
}
</style>
