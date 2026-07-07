import type { VbenFormSchema } from '#/adapter/form';

import { h } from 'vue';

import { Tag } from 'antdv-next';

type WarningWindowDay = 7 | 30 | 60 | 90;

type WarningWindowSchemaOptions = {
  dayLabels: Record<WarningWindowDay, string>;
  fieldName?: string;
  label: string;
};

type DueRiskTagOptions = {
  date?: string;
  expired?: boolean;
  expiredText: string;
  dueTodayText: string;
  expiringSoonText: string;
  remainingText: (days: number) => string;
};

const DAY_MS = 24 * 60 * 60 * 1000;
const WARNING_WINDOW_DAYS: WarningWindowDay[] = [7, 30, 60, 90];

function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function daysUntil(date?: string) {
  if (!date) {
    return undefined;
  }

  const target = new Date(`${date}T00:00:00`);
  if (Number.isNaN(target.getTime())) {
    return undefined;
  }

  return Math.round((target.getTime() - startOfToday().getTime()) / DAY_MS);
}

export function isDueWithinDays(date: string | undefined, days: number) {
  const remainDays = daysUntil(date);
  return remainDays !== undefined && remainDays >= 0 && remainDays <= days;
}

export function useWarningWindowSchema(
  options: WarningWindowSchemaOptions,
): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: false,
        options: WARNING_WINDOW_DAYS.map((day) => ({
          label: options.dayLabels[day],
          value: day,
        })),
      },
      defaultValue: 30,
      fieldName: options.fieldName ?? 'days',
      label: options.label,
    },
  ];
}

export function renderDueRiskTag(options: DueRiskTagOptions) {
  const remainDays = daysUntil(options.date);
  if (options.expired || (remainDays !== undefined && remainDays < 0)) {
    return h(Tag, { color: 'error' }, () => options.expiredText);
  }
  if (remainDays === 0) {
    return h(Tag, { color: 'error' }, () => options.dueTodayText);
  }
  if (remainDays !== undefined && remainDays <= 7) {
    return h(Tag, { color: 'warning' }, () => options.remainingText(remainDays));
  }
  return h(Tag, { color: 'processing' }, () =>
    remainDays === undefined ? options.expiringSoonText : options.remainingText(remainDays),
  );
}
