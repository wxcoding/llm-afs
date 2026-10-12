<template>
  <div class="filter-bar">
    <div class="filter-left">
      <slot name="left" />
    </div>
    <div class="filter-right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-right {
  display: flex;
  gap: 8px;
}
</style>
