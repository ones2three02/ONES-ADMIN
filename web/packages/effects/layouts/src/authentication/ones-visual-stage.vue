<script setup lang="ts">
defineOptions({
  name: 'OnesVisualStage',
});
</script>

<template>
  <div class="ecosystem" aria-hidden="true">
    <div class="city"></div>
    <div class="connector"></div>
    <div class="module m1">
      <div>
        <strong>HRMS</strong>
        <span>组织与人才管理</span>
      </div>
    </div>
    <div class="module m2">
      <div>
        <strong>WORKFLOW</strong>
        <span>流程自动化</span>
      </div>
    </div>
    <div class="module m3">
      <div>
        <strong>BI DASHBOARD</strong>
        <span>经营数据洞察</span>
      </div>
    </div>
    <div class="module m4">
      <div>
        <strong>FINANCE</strong>
        <span>财务协同管理</span>
      </div>
    </div>
    <div class="module m5">
      <div>
        <strong>RPA</strong>
        <span>自动化执行</span>
      </div>
    </div>
    <div class="module m6">
      <div>
        <strong>IAM</strong>
        <span>统一身份认证</span>
      </div>
    </div>
    <div class="core-platform">
      <div class="core-cube">
        <div>1S<small>ONES</small></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ecosystem {
  position: absolute;
  left: 4%;
  right: 6%;
  bottom: 80px;
  height: 52%;
  min-height: 340px;
  z-index: 1;
  animation: heroFloat 7s cubic-bezier(0.4, 0, 0.2, 1) infinite alternate;
  pointer-events: auto;
}

.ecosystem::before {
  content: "";
  position: absolute;
  inset: 6% 6% 4%;
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 48%, rgba(41, 117, 255, 0.22), transparent 20%),
    radial-gradient(circle at 50% 60%, rgba(94, 164, 255, 0.16), transparent 48%);
  filter: blur(20px);
}

.city {
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

.connector {
  position: absolute;
  left: 50%;
  top: 53%;
  width: 70%;
  height: 42%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(58, 141, 255, 0.28);
  border-radius: 50%;
  box-shadow: 0 0 18px rgba(39, 126, 255, 0.08);
}

.connector::before,
.connector::after {
  content: "";
  position: absolute;
  inset: 20%;
  border: 1px dashed rgba(55, 137, 255, 0.28);
  border-radius: 50%;
}

.connector::after {
  inset: 38%;
}

.core-platform {
  position: absolute;
  left: 50%;
  top: 48%;
  width: 280px;
  height: 150px;
  transform: translate(-50%, -50%);
}

.core-platform::before,
.core-platform::after {
  content: "";
  position: absolute;
  left: 50%;
  transform: translateX(-50%) rotateX(58deg) rotateZ(-1deg);
  border-radius: 34px;
}

.core-platform::before {
  bottom: 4px;
  width: 250px;
  height: 120px;
  background: linear-gradient(180deg, #fefeff, #dceaff);
  box-shadow: 0 30px 50px rgba(44, 111, 218, 0.2), inset 0 -10px 18px rgba(58, 132, 255, 0.15);
  border: 1px solid rgba(52, 118, 226, 0.15);
}

.core-platform::after {
  bottom: 28px;
  width: 190px;
  height: 92px;
  background: linear-gradient(180deg, #ffffff, #e7f1ff);
  box-shadow: 0 14px 28px rgba(32, 105, 227, 0.22), inset 0 -8px 15px rgba(58, 132, 255, 0.2);
}

.core-cube {
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
  font-weight: 850;
  font-size: 34px;
  background: linear-gradient(145deg, #2265ee, #3d96ff);
  box-shadow: 0 24px 44px rgba(32, 105, 227, 0.34), inset 0 1px 12px rgba(255, 255, 255, 0.3);
}

.core-cube small {
  display: block;
  font-size: 12px;
  letter-spacing: 0.1em;
  text-align: center;
  margin-top: 3px;
}

.module {
  position: absolute;
  width: 150px;
  height: 108px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(232, 242, 255, 0.9));
  border: 1px solid rgba(59, 118, 211, 0.12);
  box-shadow: 0 20px 38px rgba(69, 114, 180, 0.15), inset 0 -9px 18px rgba(73, 137, 237, 0.08);
  display: grid;
  place-items: center;
  text-align: center;
  color: #20416f;
  backdrop-filter: blur(12px);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.module:hover {
  transform: translateY(-6px);
  box-shadow: 0 26px 48px rgba(59, 118, 211, 0.25);
}

.module::after {
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

.module strong {
  display: block;
  font-size: 13px;
  letter-spacing: 0.03em;
}

.module span {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: #7b8ba5;
}

.m1 { left: 7%; top: 34%; }
.m2 { left: 24%; top: 3%; }
.m3 { right: 24%; top: 3%; }
.m4 { right: 7%; top: 34%; }
.m5 { left: 18%; bottom: 0; }
.m6 { right: 18%; bottom: 0; }

@keyframes heroFloat {
  from { transform: translateY(4px); }
  to { transform: translateY(-8px); }
}
</style>
