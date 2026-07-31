<script setup lang="ts">
defineOptions({
  name: 'AuthenticationFormView',
});

defineProps<{
  dataSide?: 'bottom' | 'left' | 'right' | 'top';
}>();
</script>

<template>
  <div
    class="auth-form-view relative box-border flex-col-center bg-background px-6 py-10 lg:flex-initial lg:px-8 dark:bg-background-deep"
    :data-side="dataSide"
  >
    <slot></slot>
    <!-- Router View with Transition and KeepAlive -->
    <RouterView v-slot="{ Component, route }">
      <Transition appear mode="out-in" name="slide-right">
        <KeepAlive :include="['Login']">
          <component
            :is="Component"
            :key="route.fullPath"
            class="side-content mt-6 w-full sm:mx-auto md:max-w-md"
            :data-side="dataSide"
          />
        </KeepAlive>
      </Transition>
    </RouterView>

    <!-- Footer Copyright -->

    <div
      class="absolute right-0 bottom-3 left-0 flex justify-center text-center text-xs text-muted-foreground"
    >
      <slot name="copyright"> </slot>
    </div>
  </div>
</template>

<style scoped>
.auth-form-view {
  isolation: isolate;
  overflow: hidden;
}

.auth-form-view::before,
.auth-form-view::after {
  position: absolute;
  z-index: -1;
  pointer-events: none;
  content: '';
}

.auth-form-view::before {
  top: 8%;
  right: 0;
  width: 440px;
  height: 440px;
  border-radius: 999px;
  background: radial-gradient(circle, hsl(var(--primary) / 12%), transparent 64%);
  filter: blur(8px);
  transform: translateX(38%);
}

.auth-form-view::after {
  bottom: -18%;
  left: 0;
  width: 360px;
  height: 360px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(6, 182, 212, 12%), transparent 62%);
  filter: blur(12px);
  transform: translateX(-38%);
}

.auth-form-view[data-side='right'] {
  border-left: 1px solid hsl(var(--border) / 72%);
  background:
    linear-gradient(180deg, hsl(var(--background)) 0%, hsl(var(--background-deep) / 34%) 100%),
    hsl(var(--background));
}

.auth-form-view[data-side='right']::before,
.auth-form-view[data-side='right']::after {
  display: none;
}

.side-content[data-side='right'],
.side-content[data-side='left'] {
  position: relative;
  box-sizing: border-box;
  width: min(100%, 486px);
  max-width: 486px;
  overflow: hidden;
  border: 1px solid rgba(33, 73, 126, 0.11);
  border-radius: 28px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 28px 70px rgba(60, 101, 164, 0.18);
  backdrop-filter: blur(28px);
}

.side-content[data-side='right']::before,
.side-content[data-side='left']::before {
  position: absolute;
  inset: 13px;
  z-index: -1;
  border: 1px solid hsl(var(--primary) / 8%);
  border-radius: 19px;
  pointer-events: none;
  content: '';
}

.side-content[data-side='right']::after,
.side-content[data-side='left']::after {
  position: absolute;
  top: 0;
  right: 28px;
  left: 28px;
  height: 1px;
  background: linear-gradient(90deg, transparent, hsl(var(--primary) / 38%), transparent);
  pointer-events: none;
  content: '';
}

.dark {
  .auth-form-view[data-side='right'] {
    border-left-color: rgba(255, 255, 255, 6%);
    background:
      linear-gradient(180deg, #11151d 0%, #0d1119 100%);
  }

  .side-content[data-side='right'],
  .side-content[data-side='left'] {
    border-color: rgba(255, 255, 255, 8%);
    background:
      linear-gradient(180deg, rgba(25, 30, 40, 86%), rgba(17, 20, 28, 76%)),
      rgba(17, 19, 24, 88%);
    box-shadow:
      0 36px 110px rgba(0, 0, 0, 26%),
      0 18px 56px rgba(37, 99, 235, 8%),
      inset 0 1px 0 rgba(255, 255, 255, 7%);
  }

  .side-content[data-side='right']::before,
  .side-content[data-side='left']::before {
    border-color: rgba(125, 211, 252, 8%);
  }
}

@media (max-width: 640px) {
  .side-content[data-side='right'],
  .side-content[data-side='left'] {
    max-width: none;
    border: 0;
    border-radius: 0;
    padding: 0;
    background: transparent;
    box-shadow: none;
    backdrop-filter: none;
  }

  .side-content[data-side='right']::before,
  .side-content[data-side='left']::before {
    display: none;
  }
}
</style>
