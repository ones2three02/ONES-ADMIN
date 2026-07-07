<script lang="ts" setup>
import { Button, Tag } from 'antdv-next';

import { IconifyIcon } from '@vben/icons';

interface Props {
  actionDisabled?: boolean;
  actionLoading?: boolean;
  actionText?: string;
  description?: string;
  icon?: string;
  statusColor?: string;
  statusText?: string;
  title: string;
}

withDefaults(defineProps<Props>(), {
  actionDisabled: false,
  actionLoading: false,
  actionText: '',
  description: '',
  icon: 'lucide:route',
  statusColor: 'processing',
  statusText: '',
});

defineEmits<{
  action: [];
}>();
</script>

<template>
  <div class="guided-workbench-bar">
    <div class="guided-workbench-bar__main">
      <div class="guided-workbench-bar__icon">
        <IconifyIcon :icon="icon" class="size-4" />
      </div>
      <div class="min-w-0">
        <div class="guided-workbench-bar__title-row">
          <span class="truncate font-medium">{{ title }}</span>
          <Tag v-if="statusText" :color="statusColor" class="guided-workbench-bar__tag">
            {{ statusText }}
          </Tag>
        </div>
        <div v-if="description" class="guided-workbench-bar__description">
          {{ description }}
        </div>
      </div>
    </div>

    <div v-if="$slots.actions || actionText" class="guided-workbench-bar__actions">
      <slot name="actions">
        <Button
          size="small"
          type="link"
          :disabled="actionDisabled"
          :loading="actionLoading"
          @click="$emit('action')"
        >
          {{ actionText }}
        </Button>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.guided-workbench-bar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 40px;
  margin-bottom: 8px;
  border: 1px solid hsl(var(--border) / 80%);
  border-left: 3px solid hsl(var(--primary) / 70%);
  border-radius: 6px;
  background: hsl(var(--muted) / 28%);
  padding: 7px 10px;
}

.guided-workbench-bar__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.guided-workbench-bar__icon {
  display: flex;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background: hsl(var(--primary) / 8%);
  color: hsl(var(--primary));
}

.guided-workbench-bar__title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.guided-workbench-bar__tag {
  flex: 0 0 auto;
  margin-inline-end: 0;
  line-height: 20px;
}

.guided-workbench-bar__description {
  overflow: hidden;
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guided-workbench-bar__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .guided-workbench-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .guided-workbench-bar__description {
    white-space: normal;
  }
}
</style>
