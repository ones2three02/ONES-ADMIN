<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});
</script>

<template>
  <div class="ecosystem-stage" aria-hidden="true">
    <!-- 城市底纹图案 -->
    <div class="city-skyline"></div>
    <!-- 连线轨道 -->
    <div class="connector-rings"></div>

    <!-- 6 大生态模块卡片 -->
    <div class="ecosystem-module m1">
      <div>
        <strong>HRMS</strong>
        <span>组织与人才管理</span>
      </div>
    </div>
    <div class="ecosystem-module m2">
      <div>
        <strong>WORKFLOW</strong>
        <span>流程自动化</span>
      </div>
    </div>
    <div class="ecosystem-module m3">
      <div>
        <strong>BI DASHBOARD</strong>
        <span>经营数据洞察</span>
      </div>
    </div>
    <div class="ecosystem-module m4">
      <div>
        <strong>FINANCE</strong>
        <span>财务协同管理</span>
      </div>
    </div>
    <div class="ecosystem-module m5">
      <div>
        <strong>RPA</strong>
        <span>自动化执行</span>
      </div>
    </div>
    <div class="ecosystem-module m6">
      <div>
        <strong>IAM</strong>
        <span>统一身份认证</span>
      </div>
    </div>

    <!-- 中央双层 3D 核心基座与 1S 立方体 -->
    <div class="core-platform-stage">
      <div class="core-cube-box">
        <div class="cube-inner">
          1S
          <small>ONES</small>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ecosystem-stage {
  position: absolute;
  left: 3%;
  right: 5%;
  bottom: 72px;
  height: 52%;
  z-index: 1;
  pointer-events: none;
  animation: heroFloat 7s cubic-bezier(0.2, 0.8, 0.2, 1) infinite alternate;
}

.ecosystem-stage::before {
  content: "";
  position: absolute;
  inset: 6% 6% 4%;
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 48%, rgba(41, 117, 255, 0.22), transparent 20%),
    radial-gradient(circle at 50% 60%, rgba(94, 164, 255, 0.16), transparent 48%);
  filter: blur(20px);
}

.city-skyline {
  position: absolute;
  inset: 16% 3% 2%;
  opacity: 0.34;
  background: linear-gradient(
    90deg,
    transparent 0 4%,
    rgba(155, 195, 255, 0.34) 4% 8%,
    transparent 8% 15%,
    rgba(155, 195, 255, 0.22) 15% 20%,
    transparent 20% 75%,
    rgba(155, 195, 255, 0.24) 75% 81%,
    transparent 81% 88%,
    rgba(155, 195, 255, 0.3) 88% 94%,
    transparent 94%
  );
  clip-path: polygon(
    0 100%, 0 50%, 7% 50%, 7% 32%, 12% 32%, 12% 59%, 17% 59%, 17% 23%,
    22% 23%, 22% 64%, 30% 64%, 30% 38%, 35% 38%, 35% 72%, 66% 72%, 66% 45%,
    72% 45%, 72% 60%, 78% 60%, 78% 25%, 84% 25%, 84% 52%, 90% 52%, 90% 35%,
    95% 35%, 95% 62%, 100% 62%, 100% 100%
  );
}

.connector-rings {
  position: absolute;
  left: 50%;
  top: 53%;
  width: 72%;
  height: 44%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(58, 141, 255, 0.28);
  border-radius: 50%;
  box-shadow: 0 0 18px rgba(39, 126, 255, 0.08);
}

.connector-rings::before,
.connector-rings::after {
  content: "";
  position: absolute;
  inset: 20%;
  border: 1px dashed rgba(55, 137, 255, 0.28);
  border-radius: 50%;
}

.connector-rings::after {
  inset: 38%;
}

/* 核心 3D 平台基座 */
.core-platform-stage {
  position: absolute;
  left: 50%;
  top: 48%;
  width: 280px;
  height: 150px;
  transform: translate(-50%, -50%);
}

.core-platform-stage::before,
.core-platform-stage::after {
  content: "";
  position: absolute;
  left: 50%;
  transform: translateX(-50%) rotateX(58deg) rotateZ(-1deg);
  border-radius: 34px;
}

.core-platform-stage::before {
  bottom: 4px;
  width: 250px;
  height: 120px;
  background: linear-gradient(180deg, #fefeff, #dceaff);
  box-shadow:
    0 30px 50px rgba(44, 111, 218, 0.2),
    inset 0 -10px 18px rgba(58, 132, 255, 0.15);
  border: 1px solid rgba(52, 118, 226, 0.15);
}

.core-platform-stage::after {
  bottom: 28px;
  width: 190px;
  height: 92px;
  background: linear-gradient(180deg, #ffffff, #e7f1ff);
  box-shadow:
    0 14px 28px rgba(32, 105, 227, 0.22),
    inset 0 -8px 15px rgba(58, 132, 255, 0.2);
}

.core-cube-box {
  position: absolute;
  left: 50%;
  top: 4px;
  transform: translateX(-50%);
  width: 112px;
  height: 112px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  color: white;
  background: linear-gradient(145deg, #2265ee, #3d96ff);
  box-shadow:
    0 24px 44px rgba(32, 105, 227, 0.34),
    inset 0 1px 12px rgba(255, 255, 255, 0.3);
  z-index: 5;
}

.cube-inner {
  font-weight: 850;
  font-size: 34px;
  line-height: 1;
  text-align: center;
}

.cube-inner small {
  display: block;
  font-size: 12px;
  letter-spacing: 0.1em;
  text-align: center;
  margin-top: 3px;
  font-weight: 700;
}

/* 6 大模块面板 (m1 ~ m6) */
.ecosystem-module {
  position: absolute;
  width: 156px;
  height: 110px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(232, 242, 255, 0.92));
  border: 1px solid rgba(59, 118, 211, 0.14);
  box-shadow:
    0 20px 38px rgba(69, 114, 180, 0.15),
    inset 0 -9px 18px rgba(73, 137, 237, 0.08);
  display: grid;
  place-items: center;
  text-align: center;
  color: #20416f;
  backdrop-filter: blur(12px);
  z-index: 4;
}

:deep(.dark) .ecosystem-module {
  background: linear-gradient(180deg, rgba(20, 32, 58, 0.92), rgba(15, 24, 45, 0.9));
  border-color: rgba(59, 118, 211, 0.3);
  color: #e2edff;
}

.ecosystem-module::after {
  content: "";
  position: absolute;
  left: 12%;
  right: 12%;
  bottom: -14px;
  height: 22px;
  border-radius: 50%;
  background: rgba(75, 139, 239, 0.16);
  filter: blur(8px);
}

.ecosystem-module strong {
  display: block;
  font-size: 13px;
  letter-spacing: 0.03em;
  color: #1e3a8a;
}

:deep(.dark) .ecosystem-module strong {
  color: #60a5fa;
}

.ecosystem-module span {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: #7b8ba5;
}

/* 位置网格分布 */
.m1 { left: 6%; top: 32%; }
.m2 { left: 24%; top: 2%; }
.m3 { right: 24%; top: 2%; }
.m4 { right: 6%; top: 32%; }
.m5 { left: 18%; bottom: 2px; }
.m6 { right: 18%; bottom: 2px; }

@keyframes heroFloat {
  from { transform: translateY(4px); }
  to { transform: translateY(-8px); }
}

@media (max-width: 1200px) {
  .ecosystem-stage { height: 46%; }
}
</style>
