<template>
  <div class="phoneContainer">
    <phone1v1 />
  </div>
</template>
<script setup>
import phone2v2 from './components/Phone2v2.vue';
import phone1v1 from './components/Phone1v1.vue';
import phone from './components/Phone.vue';
</script>
<style>
.phoneContainer {
  display: flex;
  width: 1200px;
  height: 100%;
  background-color: rgb(62, 69, 136);
  border: 3px solid rgb(29, 107, 158);
  border-radius: 22px;
  padding: 10px;
  margin: 10px;
}
</style>