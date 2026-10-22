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
  background: #fff;
  padding: 12px 16px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  gap: 8px;
}
</style>
