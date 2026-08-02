<script lang="ts" setup>
import { computed } from 'vue';

import { Card } from 'antdv-next';

interface Props {
  contentClass?: string;
  sideCardClass?: string;
  sideMaxWidth?: number | string;
  sideMinWidth?: number | string;
  sideTitle?: string;
  sideWidth?: number | string;
}

const props = withDefaults(defineProps<Props>(), {
  contentClass: '',
  sideCardClass: '',
  sideMaxWidth: 320,
  sideMinWidth: 240,
  sideTitle: '',
  sideWidth: 280,
});

const sideBodyStyle = {
  display: 'flex',
  flexDirection: 'column',
  minHeight: 0,
} as const;

function toCssSize(value: number | string) {
  return typeof value === 'number' ? `${value}px` : value;
}

const sideStyle = computed(() => {
  const width = toCssSize(props.sideWidth);
  return {
    flex: `0 0 ${width}`,
    maxWidth: toCssSize(props.sideMaxWidth),
    minWidth: toCssSize(props.sideMinWidth),
    width,
  };
});
</script>

<template>
  <div class="split-list-layout">
    <Card
      class="split-list-layout__aside"
      :class="sideCardClass"
      :title="sideTitle"
      :body-style="sideBodyStyle"
      :style="sideStyle"
    >
      <slot name="aside"></slot>
    </Card>

    <div class="split-list-layout__content" :class="contentClass">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.split-list-layout {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  gap: 16px;
}

.split-list-layout__aside {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.split-list-layout__aside :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
}

.split-list-layout__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

@media (max-width: 768px) {
  .split-list-layout {
    flex-direction: column;
    overflow: auto;
  }

  .split-list-layout__aside {
    width: 100% !important;
    max-width: none !important;
    flex: 0 0 auto !important;
  }

  .split-list-layout__content {
    min-height: 420px;
  }
}
</style>
