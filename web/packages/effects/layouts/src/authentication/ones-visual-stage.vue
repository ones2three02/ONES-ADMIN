<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});
</script>

<template>
  <figure class="ones-visual-stage" data-testid="brand-visual">
    <img
      alt="ONES 1S 企业协同生态"
      decoding="async"
      fetchpriority="high"
      height="1360"
      src="/brand/ones-3d-platform-4k.png"
      width="2360"
    />
  </figure>
</template>

<style scoped>
.ones-visual-stage {
  position: absolute;
  right: 4%;
  bottom: 80px;
  left: 4%;
  z-index: 1;
  height: 52%;
  min-height: 340px;
  margin: 0;
  overflow: hidden;
  pointer-events: none;
}

.ones-visual-stage img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center bottom;
}

@media (max-width: 1023px) {
  .ones-visual-stage {
    right: 2%;
    bottom: 48px;
    left: 2%;
    height: 44%;
    min-height: 260px;
  }
}

@media (max-width: 767px) {
  .ones-visual-stage {
    display: none;
  }
}
</style>
