<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="currentColor"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
    class="shield-icon"
  >
    <path
      fill-rule="evenodd"
      d="M12.516 2.17a.75.75 0 00-1.032 0 11.209 11.209 0 01-7.877 3.08.75.75 0 00-.722.515A12.74 12.74 0 002.25 9.75c0 5.592 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751a.75.75 0 00-.722-.515 11.208 11.208 0 01-7.877-3.08zm4.338 8.06a.75.75 0 00-1.06-1.06l-3.897 3.896-1.69-1.69a.75.75 0 00-1.06 1.061l2.22 2.22a.75.75 0 001.06 0l4.427-4.427z"
      clip-rule="evenodd"
    />
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 24 }
})
</script>

<style scoped>
.shield-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
