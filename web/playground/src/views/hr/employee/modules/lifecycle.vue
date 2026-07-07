<script lang="ts" setup>
import type { HrEmployeeApi } from '#/api';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { Spin, Timeline, TimelineItem } from 'antdv-next';

import { getEmployeeLifecycleEvents } from '#/api';
import { $t } from '#/locales';

const employeeName = ref('');
const events = ref<HrEmployeeApi.LifecycleEvent[]>([]);
const loading = ref(false);

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm() {
    drawerApi.close();
  },
  async onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<HrEmployeeApi.HrEmployee>();
      if (data) {
        employeeName.value = data.realName;
        loading.value = true;
        try {
          events.value = await getEmployeeLifecycleEvents(data.id);
        } catch (error) {
          console.error('Failed to load lifecycle events:', error);
        } finally {
          loading.value = false;
        }
      }
    }
  },
});

function getTimelineItemColor(type: string) {
  switch (type) {
    case 'HIRE': {
      return 'green';
    }
    case 'REGULARIZE': {
      return 'blue';
    }
    case 'TRANSFER': {
      return 'orange';
    }
    case 'RESIGN': {
      return 'red';
    }
    default: {
      return 'gray';
    }
  }
}
</script>
<template>
  <Drawer
    :title="$t('hr.employeeLifecycle.lifecycleTitle', { name: employeeName })"
    :footer="false"
  >
    <Spin :spinning="loading">
      <div class="p-6">
        <Timeline v-if="events.length > 0">
          <TimelineItem
            v-for="evt in events"
            :key="evt.id"
            :color="getTimelineItemColor(evt.eventType)"
          >
            <div class="flex flex-col gap-1">
              <span class="font-bold text-gray-800">{{ evt.summary }}</span>
              <span class="text-xs text-gray-500">
                {{
                  $t('hr.employeeLifecycle.eventDateDisplay', {
                    date: evt.eventDate,
                  })
                }}
              </span>
              <span v-if="evt.createdByName" class="text-xs text-gray-400">
                {{
                  $t('hr.employeeLifecycle.operatorDisplay', {
                    operator: evt.createdByName,
                  })
                }}
              </span>
              <span v-if="evt.detailJson && evt.detailJson !== '{}'" class="text-xs text-gray-400 bg-gray-50 p-2 rounded mt-1 border">
                {{
                  $t('hr.employeeLifecycle.detailDisplay', {
                    detail: evt.detailJson,
                  })
                }}
              </span>
            </div>
          </TimelineItem>
        </Timeline>
        <div v-else class="text-gray-400 text-center py-10">
          {{ $t('hr.employeeLifecycle.noEvents') }}
        </div>
      </div>
    </Spin>
  </Drawer>
</template>
