<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});

const nodes = [
  { id: 'hrms', label: 'HRMS', title: '组织与人才管理', pos: 'node-pos-1' },
  { id: 'iam', label: 'IAM', title: '统一身份认证防护', pos: 'node-pos-2' },
  { id: 'workflow', label: 'Workflow', title: '流程自动化驱动', pos: 'node-pos-3' },
  { id: 'ai', label: 'AI Engine', title: '智能协同引擎', pos: 'node-pos-4' },
  { id: 'bi', label: 'BI Dashboard', title: '经营数据深度洞察', pos: 'node-pos-5' },
  { id: 'rpa', label: 'RPA', title: 'RPA 机器人自动化', pos: 'node-pos-6' },
  { id: 'data', label: 'Data Asset', title: '全局数据资产中心', pos: 'node-pos-7' },
];
</script>

<template>
  <div class="hero-master-stage" aria-hidden="true">
    <!-- 高清 3D Master 渲染场景图 -->
    <div class="master-render-wrap">
      <img
        src="/brand/hero-master-bg.jpg"
        alt="ONES 3D Hero Master"
        class="master-render-img"
      />
    </div>

    <!-- 交互式 HTML 热点及 Tooltip 悬浮感应层 -->
    <div
      v-for="item in nodes"
      :key="item.id"
      class="interactive-glass-spot"
      :class="[item.pos]"
    >
      <!-- 光晕脉冲指示点 -->
      <div class="spot-pulse-ring"></div>

      <!-- HTML 动态 Hover 浮层 Tooltip -->
      <div class="node-tooltip-pop">
        <strong class="text-xs font-bold text-blue-600 dark:text-blue-400 block">{{ item.label }}</strong>
        <span class="text-[11px] text-slate-600 dark:text-slate-300 block whitespace-nowrap">{{ item.title }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero-master-stage {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.master-render-wrap {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 24px;
}

.master-render-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
  filter: drop-shadow(0 20px 40px rgba(59, 130, 246, 0.15));
}

/* 7 个 3D 玻璃节点的精确透明交互热点定位 (与 3D 背景完美贴合) */
.interactive-glass-spot {
  position: absolute;
  width: 54px;
  height: 54px;
  border-radius: 16px;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s ease;
}

.spot-pulse-ring {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  border: 1.5px solid rgba(59, 130, 246, 0.0);
  transition: all 0.3s ease;
}

.interactive-glass-spot:hover .spot-pulse-ring {
  border-color: rgba(59, 130, 246, 0.7);
  box-shadow: 0 0 18px rgba(59, 130, 246, 0.4);
  background: rgba(255, 255, 255, 0.12);
}

/* 7 个节点的精准贴合坐标 */
.node-pos-1 { left: 15%; top: 52%; }  /* HRMS (👥) */
.node-pos-2 { left: 22%; top: 68%; }  /* IAM (🛡️) */
.node-pos-3 { left: 29%; top: 32%; }  /* Workflow (🌿) */
.node-pos-4 { left: 45%; top: 22%; }  /* AI Engine (✨) */
.node-pos-5 { left: 56%; top: 32%; }  /* BI Dashboard (📊) */
.node-pos-6 { left: 66%; top: 54%; }  /* RPA (🤖) */
.node-pos-7 { left: 52%; top: 68%; }  /* Data Asset (🗄️) */

/* Tooltip 悬浮提示框 */
.node-tooltip-pop {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(4px);
  margin-bottom: 6px;
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(219, 234, 254, 0.9);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.18);
  backdrop-filter: blur(12px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.25s ease;
}

:deep(.dark) .node-tooltip-pop {
  background: rgba(15, 23, 42, 0.92);
  border-color: rgba(59, 130, 246, 0.3);
}

.interactive-glass-spot:hover .node-tooltip-pop {
  opacity: 1;
  transform: translateX(-50%) translateY(-4px);
}
</style>
