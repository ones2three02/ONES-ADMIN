<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});

const nodes = [
  { id: 'hrms', icon: 'lucide:users', label: 'HRMS', title: '组织与人才管理', pos: 'node-pos-1' },
  { id: 'workflow', icon: 'lucide:workflow', label: 'Workflow', title: '流程自动化驱动', pos: 'node-pos-2' },
  { id: 'ai', icon: 'lucide:sparkles', label: 'AI Engine', title: '智能协同引擎', pos: 'node-pos-3' },
  { id: 'bi', icon: 'lucide:bar-chart-3', label: 'BI Dashboard', title: '经营数据深度洞察', pos: 'node-pos-4' },
  { id: 'rpa', icon: 'lucide:bot', label: 'RPA', title: 'RPA 机器人自动化', pos: 'node-pos-5' },
  { id: 'iam', icon: 'lucide:shield-check', label: 'IAM', title: '企业级统一身份认证', pos: 'node-pos-6' },
  { id: 'data', icon: 'lucide:database', label: 'Data Asset', title: '数据沉淀与全局资产', pos: 'node-pos-7' },
];
</script>

<template>
  <div class="hero-master-stage" aria-hidden="true">
    <!-- 背景淡雅天际线与大气渐变纹理 -->
    <div class="master-skyline"></div>
    <div class="master-orbit-rings"></div>
    <div class="center-aura"></div>

    <!-- 纯 Icon 玻璃微悬浮节点卡片 (无文字，带有动态 HTML Tooltip) -->
    <div
      v-for="item in nodes"
      :key="item.id"
      class="glass-icon-node"
      :class="[item.pos]"
    >
      <div class="glass-icon-box">
        <span class="icon-glyph">
          <template v-if="item.id === 'hrms'">👥</template>
          <template v-else-if="item.id === 'workflow'">🌿</template>
          <template v-else-if="item.id === 'ai'">✨</template>
          <template v-else-if="item.id === 'bi'">📊</template>
          <template v-else-if="item.id === 'rpa'">🤖</template>
          <template v-else-if="item.id === 'iam'">🛡️</template>
          <template v-else-if="item.id === 'data'">🗄️</template>
        </span>
      </div>

      <!-- HTML 动态 Hover 浮层 Tooltip -->
      <div class="node-tooltip-pop">
        <strong class="text-xs font-bold text-blue-600 dark:text-blue-400 block">{{ item.label }}</strong>
        <span class="text-[11px] text-slate-600 dark:text-slate-300 block whitespace-nowrap">{{ item.title }}</span>
      </div>
    </div>

    <!-- 中央无字高质感晶体玻璃核心 (Glass Central Core) -->
    <div class="glass-master-core">
      <div class="glass-slab slab-bottom"></div>
      <div class="glass-slab slab-middle"></div>
      <div class="glass-slab slab-top">
        <div class="core-crystal-cube">
          <div class="crystal-light-ring"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero-master-stage {
  position: absolute;
  left: 4%;
  right: 6%;
  top: 36%;
  bottom: 80px;
  z-index: 2;
  pointer-events: auto;
}

.master-skyline {
  position: absolute;
  inset: 10% 0 0;
  opacity: 0.28;
  background: linear-gradient(
    90deg,
    transparent 0 4%,
    rgba(147, 197, 253, 0.3) 4% 8%,
    transparent 8% 15%,
    rgba(147, 197, 253, 0.2) 15% 20%,
    transparent 20% 75%,
    rgba(147, 197, 253, 0.25) 75% 81%,
    transparent 81% 88%,
    rgba(147, 197, 253, 0.28) 88% 94%,
    transparent 94%
  );
  clip-path: polygon(
    0 100%, 0 50%, 7% 50%, 7% 32%, 12% 32%, 12% 59%, 17% 59%, 17% 23%,
    22% 23%, 22% 64%, 30% 64%, 30% 38%, 35% 38%, 35% 72%, 66% 72%, 66% 45%,
    72% 45%, 72% 60%, 78% 60%, 78% 25%, 84% 25%, 84% 52%, 90% 52%, 90% 35%,
    95% 35%, 95% 62%, 100% 62%, 100% 100%
  );
}

.master-orbit-rings {
  position: absolute;
  left: 50%;
  top: 54%;
  width: 76%;
  height: 52%;
  transform: translate(-50%, -50%);
  border: 1px dashed rgba(59, 130, 246, 0.25);
  border-radius: 50%;
  box-shadow: 0 0 30px rgba(37, 99, 235, 0.06);
}

.master-orbit-rings::after {
  content: "";
  position: absolute;
  inset: 22%;
  border: 1px stroke rgba(96, 165, 250, 0.3);
  border-radius: 50%;
}

.center-aura {
  position: absolute;
  left: 50%;
  top: 54%;
  width: 240px;
  height: 160px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(59, 130, 246, 0.28) 0%, rgba(96, 165, 250, 0.1) 50%, transparent 75%);
  filter: blur(30px);
}

/* 纯 Icon 玻璃微悬浮节点 */
.glass-icon-node {
  position: absolute;
  z-index: 10;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.glass-icon-box {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 16px 36px rgba(59, 130, 246, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

:deep(.dark) .glass-icon-box {
  background: rgba(30, 41, 59, 0.75);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
}

.icon-glyph {
  font-size: 24px;
  line-height: 1;
  filter: drop-shadow(0 2px 4px rgba(37, 99, 235, 0.2));
}

.glass-icon-node:hover .glass-icon-box {
  transform: translateY(-6px) scale(1.08);
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(37, 99, 235, 0.4);
  box-shadow: 0 20px 42px rgba(37, 99, 235, 0.24);
}

/* Tooltip 悬浮提示框 */
.node-tooltip-pop {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(4px);
  margin-bottom: 8px;
  padding: 6px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(219, 234, 254, 0.9);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.15);
  backdrop-filter: blur(12px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.25s ease;
}

:deep(.dark) .node-tooltip-pop {
  background: rgba(15, 23, 42, 0.92);
  border-color: rgba(59, 130, 246, 0.3);
}

.glass-icon-node:hover .node-tooltip-pop {
  opacity: 1;
  transform: translateX(-50%) translateY(-2px);
}

/* 7 个 Icon 节点的环形分布 (无文字遮挡) */
.node-pos-1 { left: 8%; top: 32%; animation: floatNode 6s ease-in-out infinite; }
.node-pos-2 { left: 28%; top: 8%; animation: floatNode 7s ease-in-out 1s infinite; }
.node-pos-3 { left: 50%; top: 0%; transform: translateX(-50%); animation: floatNode 6.5s ease-in-out 2s infinite; }
.node-pos-4 { right: 28%; top: 8%; animation: floatNode 7s ease-in-out 0.5s infinite; }
.node-pos-5 { right: 8%; top: 32%; animation: floatNode 6s ease-in-out 1.5s infinite; }
.node-pos-6 { left: 20%; bottom: 6%; animation: floatNode 6.8s ease-in-out 2.5s infinite; }
.node-pos-7 { right: 20%; bottom: 6%; animation: floatNode 7.2s ease-in-out 1.2s infinite; }

/* 中央无字高质感晶体玻璃核心 */
.glass-master-core {
  position: absolute;
  left: 50%;
  top: 54%;
  width: 240px;
  height: 140px;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.glass-slab {
  position: absolute;
  left: 50%;
  transform: translateX(-50%) rotateX(58deg);
  border-radius: 28px;
  backdrop-filter: blur(16px);
}

.slab-bottom {
  bottom: 0px;
  width: 230px;
  height: 110px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(220, 235, 255, 0.85));
  box-shadow:
    0 24px 44px rgba(37, 99, 235, 0.18),
    inset 0 -8px 16px rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.slab-middle {
  bottom: 20px;
  width: 170px;
  height: 80px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(230, 240, 255, 0.9));
  box-shadow:
    0 14px 28px rgba(37, 99, 235, 0.22),
    inset 0 -6px 12px rgba(59, 130, 246, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.slab-top {
  bottom: 38px;
  width: 110px;
  height: 56px;
  background: linear-gradient(145deg, rgba(59, 130, 246, 0.85), rgba(37, 99, 235, 0.95));
  box-shadow:
    0 18px 36px rgba(37, 99, 235, 0.32),
    inset 0 2px 8px rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.core-crystal-cube {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff, #93c5fd);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.9);
  animation: crystalGlow 3s ease-in-out infinite alternate;
}

.crystal-light-ring {
  position: absolute;
  inset: -8px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  animation: ringPulse 2s linear infinite;
}

@keyframes floatNode {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}

@keyframes crystalGlow {
  from { transform: scale(0.95); opacity: 0.85; }
  to { transform: scale(1.05); opacity: 1; }
}

@keyframes ringPulse {
  0% { transform: scale(0.9); opacity: 0.3; }
  100% { transform: scale(1.3); opacity: 0; }
}
</style>
