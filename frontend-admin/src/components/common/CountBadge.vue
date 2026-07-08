<script setup>
import { computed } from 'vue';

const props = defineProps({
  count: {
    type: Number,
    required: true,
  },
  overflowCount: {
    type: Number,
    default: 99,
  },
  showZero: {
    type: Boolean,
    default: false,
  },
});

const displayCount = computed(() => {
  if (props.count > props.overflowCount) {
    return `${props.overflowCount}+`;
  }
  return props.count;
});

const isVisible = computed(() => {
  return props.count > 0 || props.showZero;
});
</script>

<template>
  <div class="position-relative d-inline-block">
    <slot></slot>
    <span
      v-if="isVisible"
      class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger shadow-sm z-1"
      style="font-size: 0.7rem; padding: 0.25em 0.5em;"
    >
      {{ displayCount }}
    </span>
  </div>
</template>

<style scoped>
/* Ensure badge always stays on top of the slotted element */
.badge {
  z-index: 10;
}
</style>
