<template>
  <div class="form-item">
    <div class="form-label" :style="labelStyle">{{ label }}</div>
    <slot></slot>
  </div>
</template>

<script>
export default {
  name: 'FormItem',
  props: {
    label: {
      type: String,
      default: ''
    },
    labelWidth: {
      type: [Number],
      default: 80
    }
  },
  computed: {
    labelStyle() {
      return {
        width: `${ this.labelWidth }px`
      }
    }
  }
}
</script>

<style scoped lang="less">
@import "../assets/style/var";

.form-item {
  margin-top: 16px;
  margin-right: 60px;
  display: flex;
  align-items: center;

  .form-label {
    flex: 0 0 auto;
    width: 80px;
    text-align: right;
    margin-right: 10px;
    word-wrap: break-word;
    font-size: 14px;
    line-height: 32px;
    color: @--color-text-primary;
  }
}

.form-item + .form-item {
  //margin-r: 60px;
}

.form-item:nth-last-child(2) {
  margin-right: 180px !important;
}
</style>
