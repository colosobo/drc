<template>
  <div class="card">
    <div class="card-title">{{ title }}</div>
    <div class="card-body">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: ''
    }
  },
  name: 'Card'
}
</script>

<style scoped lang="less">
@import "../assets/style/var";

.card {
  margin-bottom: 20px;
  box-sizing: border-box;
  color: white;
  background: #06021a;
  //box-shadow: 0px 2px 4px 0px rgba(1, 1, 1, 0.04);
  border-radius: 2px;

  .card-title {
    padding: 16px 20px;
    line-height: 22px;
    font-size: 19px;
    font-weight: 500;
    color: #ef891e;
    border-bottom: 1px solid #081451;
  }

  .card-body {
    padding: 20px;
  }

}

.card + .card {
  margin-bottom: 0px;
}
</style>
