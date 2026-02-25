<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

type TimerMode = 'STOPWATCH' | 'COUNTDOWN'

const mode = ref<TimerMode>('STOPWATCH')
const isRunning = ref(false)
const showPanel = ref(false)
const hasCountdownStarted = ref(false)

// 计时 / 倒计时的时间
const elapsedSeconds = ref(0) // 计时
const remainingSeconds = ref(60 * 60) // 倒计时当前剩余

// 倒计时预设（小时、分钟）
const presetHours = ref(1)
const presetMinutes = ref(0)

// 小时输入展示：始终两位数（例如 01、09、12）
const presetHoursDisplay = computed({
  get: () => String(Math.max(0, Math.min(99, presetHours.value))).padStart(2, '0'),
  set: (val: string) => {
    const numeric = parseInt(val.replace(/[^\d]/g, ''), 10)
    if (Number.isNaN(numeric)) {
      presetHours.value = 0
    } else {
      presetHours.value = Math.max(0, Math.min(99, numeric))
    }
  },
})

// 分钟输入展示：始终两位数（例如 01、09、45）
const presetMinutesDisplay = computed({
  get: () => String(Math.max(0, Math.min(59, presetMinutes.value))).padStart(2, '0'),
  set: (val: string) => {
    const numeric = parseInt(val.replace(/[^\d]/g, ''), 10)
    if (Number.isNaN(numeric)) {
      presetMinutes.value = 0
    } else {
      presetMinutes.value = Math.max(0, Math.min(59, numeric))
    }
  },
})

let timerId: number | null = null
let hoverHideTimeout: number | null = null

const formattedTime = computed(() => {
  const total =
    mode.value === 'STOPWATCH' ? elapsedSeconds.value : Math.max(remainingSeconds.value, 0)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(h)}:${pad(m)}:${pad(s)}`
})

const isCountdownFinished = computed(
  () => mode.value === 'COUNTDOWN' && remainingSeconds.value <= 0,
)

const barIsRunning = computed(() => isRunning.value && !isCountdownFinished.value)

const clearTimer = () => {
  if (timerId !== null) {
    window.clearInterval(timerId)
    timerId = null
  }
}

const tick = () => {
  if (mode.value === 'STOPWATCH') {
    elapsedSeconds.value += 1
  } else {
    remainingSeconds.value -= 1
    if (remainingSeconds.value <= 0) {
      remainingSeconds.value = 0
      isRunning.value = false
      clearTimer()
      hasCountdownStarted.value = false
    }
  }
}

const startTimer = () => {
  if (barIsRunning.value) return
  if (mode.value === 'COUNTDOWN') {
    // 首次开始或已结束时，使用当前预设时间重新计算
    const total = presetHours.value * 3600 + presetMinutes.value * 60
    if (!hasCountdownStarted.value || remainingSeconds.value <= 0) {
      if (total <= 0) return
      remainingSeconds.value = total
    }
    hasCountdownStarted.value = true
  }
  isRunning.value = true
  clearTimer()
  timerId = window.setInterval(tick, 1000)
}

const pauseTimer = () => {
  isRunning.value = false
  clearTimer()
}

const resetTimer = () => {
  if (mode.value === 'STOPWATCH') {
    elapsedSeconds.value = 0
  } else {
    remainingSeconds.value = presetHours.value * 3600 + presetMinutes.value * 60
  }
  isRunning.value = false
  hasCountdownStarted.value = false
  clearTimer()
}

const switchMode = (target: TimerMode) => {
  if (mode.value === target) return
  mode.value = target
  // 切换模式时重置状态
  if (target === 'STOPWATCH') {
    elapsedSeconds.value = 0
  } else {
    remainingSeconds.value = presetHours.value * 3600 + presetMinutes.value * 60
  }
  isRunning.value = false
  hasCountdownStarted.value = false
  clearTimer()
}

const handleHoverEnter = () => {
  if (hoverHideTimeout !== null) {
    window.clearTimeout(hoverHideTimeout)
    hoverHideTimeout = null
  }
  showPanel.value = true
}

const handleHoverLeave = () => {
  if (hoverHideTimeout !== null) {
    window.clearTimeout(hoverHideTimeout)
  }
  hoverHideTimeout = window.setTimeout(() => {
    showPanel.value = false
    hoverHideTimeout = null
  }, 80)
}

const handleHoursWheel = (event: WheelEvent) => {
  event.preventDefault()
  if (event.deltaY > 0) {
    // 向下滚动，减小
    presetHours.value = Math.max(0, presetHours.value - 1)
  } else if (event.deltaY < 0) {
    // 向上滚动，增大
    presetHours.value = Math.min(99, presetHours.value + 1)
  }
}

const handleMinutesWheel = (event: WheelEvent) => {
  event.preventDefault()
  if (event.deltaY > 0) {
    // 向下滚动：在 0 时跳到 59，否则正常减 1
    if (presetMinutes.value === 0) {
      presetMinutes.value = 59
    } else {
      presetMinutes.value = Math.max(0, presetMinutes.value - 1)
    }
  } else if (event.deltaY < 0) {
    // 向上滚动：在 59 时跳到 0，否则正常加 1（0 ~ 59 之间循环）
    if (presetMinutes.value === 59) {
      presetMinutes.value = 0
    } else {
      presetMinutes.value = Math.min(59, presetMinutes.value + 1)
    }
  }
}

const clampPresetHours = () => {
  if (presetHours.value < 0) {
    presetHours.value = 0
  } else if (presetHours.value > 99) {
    presetHours.value = 99
  }
}

const clampPresetMinutes = () => {
  if (presetMinutes.value < 0) {
    presetMinutes.value = 0
  } else if (presetMinutes.value > 59) {
    presetMinutes.value = 59
  }
}

onBeforeUnmount(() => {
  clearTimer()
  if (hoverHideTimeout !== null) {
    window.clearTimeout(hoverHideTimeout)
    hoverHideTimeout = null
  }
})
</script>

<template>
  <div class="timer-wrapper">
    <div
      class="timer-bar"
      :class="{ 'timer-bar--running': barIsRunning, 'timer-bar--countdown': mode === 'COUNTDOWN' }"
      @mouseenter="handleHoverEnter"
      @mouseleave="handleHoverLeave"
    >
      <button
        type="button"
        class="timer-icon-btn"
        @click.stop="barIsRunning ? pauseTimer() : startTimer()"
      >
        <span v-if="!barIsRunning" class="timer-icon-play">▶</span>
        <span v-else class="timer-icon-pause">⏸</span>
      </button>
      <span class="timer-time">{{ formattedTime }}</span>
      <button type="button" class="timer-icon-btn" title="重置" @click.stop="resetTimer">
        ⟳
      </button>
    </div>

    <div
      class="timer-hover-bridge"
      @mouseenter="handleHoverEnter"
      @mouseleave="handleHoverLeave"
    ></div>

    <transition name="fade">
      <div
        v-if="showPanel"
        class="timer-panel"
        @mouseenter="handleHoverEnter"
        @mouseleave="handleHoverLeave"
      >
        <div class="timer-tabs">
          <button
            type="button"
            class="timer-tab"
            :class="{ 'timer-tab--active': mode === 'STOPWATCH' }"
            @click="switchMode('STOPWATCH')"
          >
            计时
          </button>
          <button
            type="button"
            class="timer-tab"
            :class="{ 'timer-tab--active': mode === 'COUNTDOWN' }"
            @click="switchMode('COUNTDOWN')"
          >
            倒计时
          </button>
        </div>

        <div v-if="mode === 'STOPWATCH'" class="panel-content">
          <div class="panel-time">{{ formattedTime }}</div>
          <button
            type="button"
            class="panel-primary-btn"
            @click="barIsRunning ? pauseTimer() : startTimer()"
          >
            {{ barIsRunning ? '暂停计时' : '开始计时' }}
          </button>
          <button type="button" class="panel-link-btn" @click="resetTimer">
            重置计时
          </button>
        </div>

        <div v-else class="panel-content">
          <div class="countdown-inputs">
            <input
              v-model="presetHoursDisplay"
              type="text"
              min="0"
              max="99"
              class="time-input"
              @wheel.prevent="handleHoursWheel"
              @change="clampPresetHours"
              @blur="clampPresetHours"
            />
            <span class="time-input-label">时</span>
            <input
              v-model="presetMinutesDisplay"
              type="text"
              min="0"
              max="59"
              class="time-input"
              @wheel.prevent="handleMinutesWheel"
              @change="clampPresetMinutes"
              @blur="clampPresetMinutes"
            />
            <span class="time-input-label">分</span>
          </div>
          <button
            type="button"
            class="panel-primary-btn"
            @click="startTimer"
          >
            {{ isCountdownFinished ? '重新开始倒计时' : '开始倒计时' }}
          </button>
          <button type="button" class="panel-link-btn" @click="resetTimer">
            重置倒计时
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.timer-wrapper {
  position: relative;
  margin-right: 8px;
}

.timer-hover-bridge {
  position: absolute;
  top: 24px;
  left: -48px;
  width: 220px;
  height: 6px;
  pointer-events: auto;
  background-color: transparent;
}

.timer-bar {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  font-size: 12px;
  color: #4b5563;
}

.timer-bar--running {
  background-color: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.timer-bar--countdown {
  color: #d97706;
}

.timer-icon-btn {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  color: inherit;
}

.timer-time {
  font-variant-numeric: tabular-nums;
  min-width: 74px;
  text-align: center;
}

.timer-panel {
  position: absolute;
  top: 30px;
  left: -48px;
  width: 220px;
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  padding: 10px 12px 12px;
  z-index: 30;
}

.timer-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 8px;
}

.timer-tab {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 6px 0;
  font-size: 13px;
  cursor: pointer;
  background-color: #ffffff;
}

.timer-tab--active {
  border-color: #2563eb;
  background-color: #eff6ff;
  color: #2563eb;
}

.panel-content {
  text-align: center;
}

.panel-time {
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
}

.panel-primary-btn {
  width: 100%;
  border-radius: 999px;
  padding: 6px 0;
  border: none;
  background-color: #111827;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  margin-top: 4px;
}

.panel-primary-btn:hover {
  background-color: #030712;
}

.panel-link-btn {
  margin-top: 6px;
  border: none;
  background: transparent;
  color: #ef4444;
  font-size: 12px;
  cursor: pointer;
}

.panel-link-btn:hover {
  color: #b91c1c;
}

.countdown-inputs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
}

.time-input {
  width: 40px;
  padding: 4px 4px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  text-align: center;
  font-size: 13px;
}

.time-input::-webkit-outer-spin-button,
.time-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.time-input[type='number'] {
  appearance: textfield;
  -moz-appearance: textfield;
}

.time-input-label {
  font-size: 12px;
  color: #6b7280;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

