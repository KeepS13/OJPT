<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="home">
    <div class="hero">
      <div class="hero__copy">
        <span class="hero__eyebrow">Personal Training Desk</span>
        <h1 class="hero__title">一个更像你自己练算法时会打开的平台。</h1>
        <p class="hero__subtitle">
          少一点平台感，多一点训练感。直接进题、直接写代码、直接按标准输入输出思考，保持节奏，把注意力留给题目本身。
        </p>

        <div class="hero__actions">
          <RouterLink to="/problemset" class="hero__button hero__button--primary">
            开始今日训练
          </RouterLink>
          <RouterLink to="/profile" class="hero__button hero__button--secondary">
            查看个人状态
          </RouterLink>
        </div>

        <div class="hero__chips">
          <span class="hero__chip">标准输入输出</span>
          <span class="hero__chip">题号直达</span>
          <span class="hero__chip">专注个人练习</span>
        </div>
      </div>

      <div class="hero__panel">
        <div class="panel-card panel-card--terminal">
          <div class="panel-card__header">
            <span class="panel-dot panel-dot--red" />
            <span class="panel-dot panel-dot--amber" />
            <span class="panel-dot panel-dot--green" />
            <span class="panel-card__title">训练输入示例</span>
          </div>
          <pre class="terminal">
请输入数组长度：
4
请输入数组元素：
2 7 11 15
请输入目标值：
9
输出结果：
0 1</pre
          >
        </div>

        <div class="panel-card panel-card--routine">
          <div class="routine-title">推荐练习路径</div>
          <ol class="routine-list">
            <li>进入题库，按题号或难度挑选一道题</li>
            <li>阅读题面，先按控制台输入输出组织思路</li>
            <li>完成代码后再切换下一题，保持连续训练节奏</li>
          </ol>
        </div>
      </div>
    </div>

    <div class="feature-grid">
      <article class="feature-card">
        <span class="feature-card__index">01</span>
        <h2 class="feature-card__title">像本地练习一样输入</h2>
        <p class="feature-card__text">
          模板采用控制台读写，不依赖函数签名，减少“题解模式”和“实际提交模式”的切换成本。
        </p>
      </article>

      <article class="feature-card">
        <span class="feature-card__index">02</span>
        <h2 class="feature-card__title">保持题面专注</h2>
        <p class="feature-card__text">
          题号、难度、标签和题面信息集中展示，首页只做训练入口，不堆叠多余运营信息。
        </p>
      </article>

      <article class="feature-card">
        <span class="feature-card__index">03</span>
        <h2 class="feature-card__title">给管理端留最小闭环</h2>
        <p class="feature-card__text">
          题目维护和标签管理存在，但它们服务的是训练内容本身，不再喧宾夺主。
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.home {
  min-height: 100%;
  padding: 18px 14px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.12), transparent 26%),
    linear-gradient(180deg, #f8fafc 0%, #edf7f3 100%);
  border-radius: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  align-items: stretch;
}

.hero__copy,
.hero__panel,
.feature-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 16px 50px rgba(15, 23, 42, 0.06);
}

.hero__copy {
  padding: 30px 28px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 247, 245, 0.92)),
    linear-gradient(120deg, rgba(22, 163, 74, 0.05), rgba(8, 145, 178, 0.05));
}

.hero__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #166534;
  background: rgba(34, 197, 94, 0.1);
}

.hero__title {
  margin: 16px 0 14px;
  font-size: clamp(30px, 5vw, 52px);
  line-height: 1.06;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.hero__subtitle {
  max-width: 640px;
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #475569;
}

.hero__actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 136px;
  padding: 12px 18px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    background-color 0.16s ease;
}

.hero__button:hover {
  transform: translateY(-1px);
}

.hero__button--primary {
  color: #ffffff;
  background: linear-gradient(135deg, #166534, #0f766e);
  box-shadow: 0 14px 32px rgba(22, 101, 52, 0.2);
}

.hero__button--secondary {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.9);
}

.hero__chips {
  margin-top: 26px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero__chip {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.hero__panel {
  padding: 18px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.97)),
    linear-gradient(135deg, rgba(34, 197, 94, 0.08), rgba(56, 189, 248, 0.08));
  display: grid;
  gap: 14px;
}

.panel-card {
  border-radius: 18px;
  overflow: hidden;
}

.panel-card--terminal {
  background: rgba(15, 23, 42, 0.76);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.panel-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.panel-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.panel-dot--red {
  background: #f87171;
}

.panel-dot--amber {
  background: #fbbf24;
}

.panel-dot--green {
  background: #4ade80;
}

.panel-card__title {
  margin-left: 4px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.9);
}

.terminal {
  margin: 0;
  padding: 16px 18px 18px;
  color: #dcfce7;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.panel-card--routine {
  padding: 16px 18px 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(226, 232, 240, 0.96));
}

.routine-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.routine-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.8;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.feature-card {
  padding: 18px 18px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
}

.feature-card__index {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #0f766e;
}

.feature-card__title {
  margin: 10px 0 8px;
  font-size: 18px;
  color: #111827;
}

.feature-card__text {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: #64748b;
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .home {
    padding: 12px 8px 20px;
    border-radius: 14px;
  }

  .hero__copy,
  .hero__panel,
  .feature-card {
    border-radius: 18px;
  }

  .hero__copy {
    padding: 20px;
  }

  .hero__actions {
    flex-direction: column;
  }

  .hero__button {
    width: 100%;
  }
}
</style>
