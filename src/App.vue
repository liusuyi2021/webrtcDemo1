<template>
  <div class="phoneContainer">
    <phone1v1 />
  </div>
</template>
<script setup>

import phone1v1 from './components/Phone1V1.vue';

</script>
<style>
.phoneContainer {
  display: flex;
  width: 100%;
  height: 100%;
  background-color: rgb(62, 69, 136);
  border: 3px solid rgb(29, 107, 158);
  border-radius: 22px;
  padding: 10px;
  margin: 10px;
}
</style>