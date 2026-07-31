<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});
</script>

<template>
  <div class="ones-visual-stage-container">
    <!-- 背景等轴测网格光效 -->
    <div class="isometric-grid-bg"></div>
    <div class="glow-backdrop"></div>

    <!-- 3D 节点图结构 -->
    <div class="isometric-stage">
      <!-- 关联光束线条 (SVG overlay) -->
      <svg class="connecting-lines" viewBox="0 0 700 420" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M 140 130 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />
        <path d="M 310 110 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />
        <path d="M 540 140 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />
        <path d="M 160 300 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />
        <path d="M 330 330 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />
        <path d="M 520 310 L 350 210" stroke="url(#blue-line)" stroke-width="2" stroke-dasharray="4 4" />

        <defs>
          <linearGradient id="blue-line" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.2" />
            <stop offset="50%" stop-color="#2563eb" stop-opacity="0.8" />
            <stop offset="100%" stop-color="#60a5fa" stop-opacity="0.3" />
          </linearGradient>
        </defs>
      </svg>

      <!-- 节点 1: HRMS (左上) -->
      <div class="node-card node-hrms float-anim-1">
        <div class="node-header">
          <span class="node-tag">HRMS</span>
          <span class="node-status"></span>
        </div>
        <div class="node-preview hrms-preview">
          <div class="avatar-circle"></div>
          <div class="skeleton-lines">
            <span class="sk-line w-full"></span>
            <span class="sk-line w-3/4"></span>
          </div>
        </div>
      </div>

      <!-- 节点 2: WORKFLOW (中上) -->
      <div class="node-card node-workflow float-anim-2">
        <div class="node-header">
          <span class="node-tag">WORKFLOW</span>
        </div>
        <div class="node-preview workflow-preview">
          <div class="wf-box"></div>
          <div class="wf-arrow">↓</div>
          <div class="wf-boxes">
            <span></span>
            <span></span>
          </div>
        </div>
      </div>

      <!-- 节点 3: FIN NANCE (右上) -->
      <div class="node-card node-finance float-anim-3">
        <div class="node-header">
          <span class="node-tag">FIN NANCE</span>
        </div>
        <div class="node-preview finance-preview">
          <div class="pie-chart"></div>
        </div>
      </div>

      <!-- 中央核心 1S 3D 立方体舞台 (Center 1S Cube) -->
      <div class="central-core float-core">
        <div class="core-platform-ring ring-3"></div>
        <div class="core-platform-ring ring-2"></div>
        <div class="core-platform-ring ring-1"></div>

        <!-- 3D 1S 立方体 -->
        <div class="cube-3d">
          <div class="cube-face face-front">1S</div>
          <div class="cube-face face-back">1S</div>
          <div class="cube-face face-right">1S</div>
          <div class="cube-face face-left">1S</div>
          <div class="cube-face face-top">1S</div>
          <div class="cube-face face-bottom"></div>
        </div>

        <div class="core-glow-light"></div>
      </div>

      <!-- 节点 4: BI DASHBOARD (左下) -->
      <div class="node-card node-bi float-anim-2">
        <div class="node-header">
          <span class="node-tag">BI DASHBOARD</span>
        </div>
        <div class="node-preview bi-preview">
          <div class="bar-chart">
            <span style="height: 40%"></span>
            <span style="height: 75%"></span>
            <span style="height: 55%"></span>
            <span style="height: 90%"></span>
          </div>
        </div>
      </div>

      <!-- 节点 5: RPA (中下) -->
      <div class="node-card node-rpa float-anim-1">
        <div class="node-header">
          <span class="node-tag">RPA</span>
        </div>
        <div class="node-preview rpa-preview">
          <div class="rpa-robot-icon">
            🤖
          </div>
        </div>
      </div>

      <!-- 节点 6: IAM (右下) -->
      <div class="node-card node-iam float-anim-3">
        <div class="node-header">
          <span class="node-tag">IAM</span>
        </div>
        <div class="node-preview iam-preview">
          <div class="shield-icon">
            🛡️
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ones-visual-stage-container {
  position: relative;
  width: 100%;
  height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  perspective: 1000px;
}

.isometric-grid-bg {
  position: absolute;
  inset: -10%;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.06) 1px, transparent 1px);
  background-size: 36px 36px;
  transform: rotateX(55deg) rotateZ(-25deg);
  pointer-events: none;
  opacity: 0.6;
}

.glow-backdrop {
  position: absolute;
  width: 320px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.25) 0%, rgba(37, 99, 235, 0.05) 60%, transparent 80%);
  filter: blur(40px);
  pointer-events: none;
}

.isometric-stage {
  position: relative;
  width: 660px;
  height: 360px;
}

.connecting-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

/* 节点卡片通用设计 */
.node-card {
  position: absolute;
  z-index: 2;
  width: 130px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(219, 234, 254, 0.9);
  box-shadow:
    0 12px 30px -8px rgba(37, 99, 235, 0.15),
    0 4px 10px -2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}

:deep(.dark) .node-card {
  background: rgba(30, 41, 59, 0.85);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 12px 30px -8px rgba(0, 0, 0, 0.5);
}

.node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.node-tag {
  font-size: 11px;
  font-weight: 800;
  color: #2563eb;
  letter-spacing: 0.5px;
}

:deep(.dark) .node-tag {
  color: #60a5fa;
}

.node-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}

.node-preview {
  height: 48px;
  background: rgba(241, 245, 249, 0.8);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 4px;
}

:deep(.dark) .node-preview {
  background: rgba(15, 23, 42, 0.6);
}

/* 各个节点的特化预览图示 */
.hrms-preview {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 0 8px;
}
.avatar-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}
.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sk-line {
  height: 4px;
  background: rgba(148, 163, 184, 0.4);
  border-radius: 2px;
}

.workflow-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 9px;
  color: #3b82f6;
}
.wf-box {
  width: 36px;
  height: 10px;
  border: 1px stroke #3b82f6;
  background: #dbeafe;
  border-radius: 3px;
}
.wf-arrow {
  line-height: 1;
  font-weight: bold;
}
.wf-boxes {
  display: flex;
  gap: 6px;
}
.wf-boxes span {
  width: 16px;
  height: 10px;
  background: #3b82f6;
  border-radius: 3px;
}

.finance-preview {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pie-chart {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: conic-gradient(#2563eb 0 65%, #60a5fa 65% 85%, #93c5fd 85% 100%);
  box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.6);
}

.bi-preview {
  padding: 0 12px;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  width: 100%;
  height: 32px;
}
.bar-chart span {
  flex: 1;
  background: linear-gradient(180deg, #60a5fa 0%, #2563eb 100%);
  border-radius: 2px 2px 0 0;
}

.rpa-preview, .iam-preview {
  font-size: 22px;
}

/* 位置分布 (根据 UI 图精准分布) */
.node-hrms {
  top: 30px;
  left: 30px;
}
.node-workflow {
  top: 10px;
  left: 260px;
}
.node-finance {
  top: 35px;
  right: 30px;
}
.node-bi {
  bottom: 35px;
  left: 40px;
}
.node-rpa {
  bottom: 15px;
  left: 270px;
}
.node-iam {
  bottom: 30px;
  right: 40px;
}

/* 中央 3D 核心区 */
.central-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
  width: 160px;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.core-platform-ring {
  position: absolute;
  border-radius: 50%;
  transform: rotateX(60deg) rotateZ(0deg);
  border: 1.5px solid rgba(59, 130, 246, 0.4);
}
.ring-1 {
  width: 110px;
  height: 110px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.25) 0%, transparent 70%);
  box-shadow: 0 0 15px rgba(37, 99, 235, 0.4);
}
.ring-2 {
  width: 140px;
  height: 140px;
  border-style: dashed;
  border-color: rgba(96, 165, 250, 0.5);
  animation: ring-spin 20s linear infinite;
}
.ring-3 {
  width: 170px;
  height: 170px;
  border-color: rgba(147, 197, 253, 0.2);
}

/* 3D 核心蓝立方体 */
.cube-3d {
  width: 60px;
  height: 60px;
  position: relative;
  transform-style: preserve-3d;
  transform: rotateX(-25deg) rotateY(45deg);
  animation: cube-rotate 12s ease-in-out infinite alternate;
}

.cube-face {
  position: absolute;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 900;
  color: #ffffff;
  letter-spacing: -0.5px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: inset 0 0 20px rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(4px);
}

.face-front  { background: linear-gradient(135deg, #3b82f6, #1d4ed8); transform: translateZ(30px); }
.face-back   { background: linear-gradient(135deg, #1d4ed8, #1e40af); transform: rotateY(180deg) translateZ(30px); }
.face-right  { background: linear-gradient(135deg, #2563eb, #1e40af); transform: rotateY(90deg) translateZ(30px); }
.face-left   { background: linear-gradient(135deg, #60a5fa, #2563eb); transform: rotateY(-90deg) translateZ(30px); }
.face-top    { background: linear-gradient(135deg, #93c5fd, #3b82f6); transform: rotateX(90deg) translateZ(30px); }
.face-bottom { background: rgba(30, 58, 138, 0.8); transform: rotateX(-90deg) translateZ(30px); }

.core-glow-light {
  position: absolute;
  bottom: 10px;
  width: 80px;
  height: 30px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.6);
  filter: blur(12px);
  transform: rotateX(60deg);
}

/* 浮动动画 */
@keyframes float-anim {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-8px); }
}

.float-anim-1 { animation: float-anim 5s ease-in-out infinite; }
.float-anim-2 { animation: float-anim 6s ease-in-out 1s infinite; }
.float-anim-3 { animation: float-anim 5.5s ease-in-out 2s infinite; }
.float-core   { animation: float-anim 4s ease-in-out infinite; }

@keyframes ring-spin {
  from { transform: rotateX(60deg) rotateZ(0deg); }
  to   { transform: rotateX(60deg) rotateZ(360deg); }
}

@keyframes cube-rotate {
  0% { transform: rotateX(-22deg) rotateY(35deg); }
  50% { transform: rotateX(-30deg) rotateY(65deg); }
  100% { transform: rotateX(-18deg) rotateY(15deg); }
}
</style>
