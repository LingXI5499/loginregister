import { computed, onBeforeUnmount, ref } from 'vue'

export function useCountdown(defaultSeconds = 60) {
  const seconds = ref(0)
  let timer = null
  const running = computed(() => seconds.value > 0)
  const text = computed(() => running.value ? `${seconds.value}s 后重试` : '发送验证码')

  function start(value = defaultSeconds) {
    stop()
    seconds.value = value
    timer = window.setInterval(() => {
      seconds.value -= 1
      if (seconds.value <= 0) stop()
    }, 1000)
  }

  function stop() {
    if (timer) {
      window.clearInterval(timer)
      timer = null
    }
    seconds.value = 0
  }

  onBeforeUnmount(stop)
  return { seconds, running, text, start, stop }
}
