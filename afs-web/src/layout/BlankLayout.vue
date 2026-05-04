<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'BlankLayout'
}
</script>

<style scoped>
.blank-layout {
  min-height: 100vh;
}
</style>
