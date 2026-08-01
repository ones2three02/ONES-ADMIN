<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});
</script>

<template>
  <div class="ones-3d-stage">
    <img
      src="/brand/ones-3d-platform.png"
      alt="ONES 3D Platform"
      class="ones-3d-platform-img"
    />
  </div>
</template>

<style scoped>
.ones-3d-stage {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ones-3d-platform-img {
  width: 100%;
  max-width: 650px;
  height: auto;
  object-fit: contain;
  mix-blend-mode: multiply;
  filter: contrast(1.02) brightness(1.01);
}

:deep(.dark) .ones-3d-platform-img {
  mix-blend-mode: normal;
  filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.4));
}
</style>
