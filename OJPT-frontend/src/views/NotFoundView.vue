<template>
  <section class="not-found">
    <h1 class="code">404</h1>
    <p class="title">页面走丢了</p>
    <p class="desc">你访问的页面不存在，或者已经被移除。</p>
    <RouterLink to="/" class="back-link">返回首页</RouterLink>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.not-found {
  padding: 80px 0;
  text-align: center;
}

.code {
  font-size: 64px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.title {
  font-size: 20px;
  margin-bottom: 8px;
}

.desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 24px;
}

.back-link {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 999px;
  border: 1px solid #2563eb;
  color: #2563eb;
  font-size: 14px;
}

.back-link:hover {
  background-color: #eff6ff;
}
</style>


