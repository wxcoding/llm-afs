<template>
  <div class="brand-hero compact">
    <div class="hero-deco deco-1"></div>
    <div class="hero-deco deco-2"></div>
    <div class="hero-content">
      <span v-if="badge" class="hero-badge">{{ badge }}</span>
      <h2>{{ title }}</h2>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  badge: { type: String, default: '' }
})
</script>
