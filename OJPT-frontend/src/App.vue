<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'
import TopNav from '@/components/layout/TopNav.vue'

const route = useRoute()

const showTopNav = computed(() => {
  return route.meta.hideTopNav !== true
})
</script>

<template>
  <TopNav v-if="showTopNav" />
  <main class="main-view">
    <RouterView />
  </main>
</template>

<style scoped>
.main-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px 10px 0;
  overflow: auto;
  box-sizing: border-box;
}
</style>

