<script setup lang="ts">
defineOptions({
  name: 'AuthenticationFormView',
});

defineProps<{
  dataSide?: 'bottom' | 'left' | 'right' | 'top';
}>();
</script>

<template>
  <div class="auth-form-view" :data-side="dataSide">
    <slot></slot>
    <RouterView v-slot="{ Component, route }">
      <Transition appear mode="out-in" name="slide-right">
        <KeepAlive :include="['Login']">
          <component
            :is="Component"
            :key="route.fullPath"
            class="side-content"
            :data-side="dataSide"
          />
        </KeepAlive>
      </Transition>
    </RouterView>

    <div class="auth-form-copyright">
      <slot name="copyright"></slot>
    </div>
  </div>
</template>

<style scoped>
.auth-form-view {
  display: flex;
  box-sizing: border-box;
  min-width: 0;
  align-items: flex-start;
  justify-content: center;
  overflow: hidden;
  padding: min(18vh, 184px) clamp(38px, 5vw, 78px) 106px;
}

.side-content {
  box-sizing: border-box;
  width: min(100%, 478px);
  max-width: 478px;
  min-height: min(696px, calc(100dvh - 220px));
  margin: 0 !important;
  overflow: hidden;
  border: 1px solid rgba(222, 228, 238, 0.72);
  border-radius: 20px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 22px 54px rgba(70, 91, 126, 0.16);
  backdrop-filter: blur(24px);
}

.auth-form-copyright {
  position: absolute;
  right: 0;
  bottom: 12px;
  left: 0;
  display: flex;
  justify-content: center;
  color: #748197;
  font-size: 12px;
}

@media (max-height: 900px) and (min-width: 1280px) {
  .auth-form-view {
    padding-top: clamp(82px, 15vh, 135px);
    padding-bottom: 76px;
  }

  .side-content {
    min-height: 0;
    max-height: calc(100dvh - 158px);
    overflow-y: auto;
  }
}

@media (max-width: 1279px) {
  .auth-form-view {
    align-items: center;
    padding: 88px 32px 72px;
  }

  .side-content {
    min-height: 0;
  }
}

@media (max-width: 640px) {
  .auth-form-view {
    padding: 84px 16px 64px;
  }

  .side-content {
    width: 100%;
    max-width: 478px;
    max-height: calc(100dvh - 148px);
    overflow-x: hidden;
    overflow-y: auto;
    border-radius: 18px;
    padding: 24px 20px;
  }
}
</style>
