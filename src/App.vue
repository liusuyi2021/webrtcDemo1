<template>
  <div>
    <Phone />
  </div>
</template>

<script setup>
import Phone from './components/Phone.vue';
</script>

<style scoped>
.el-container {
  position: absolute;
  right: 0;
  left: 0;
  top: 0;
  bottom: 0;
}

/* 添加基本样式以模拟苹果手机界面 */
#app {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f0f0;
}
</style>
