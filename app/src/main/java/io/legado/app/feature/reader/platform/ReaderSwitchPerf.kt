package io.legado.app.feature.reader.platform

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.legado.app.constant.AppLog

/**
 * 开书/关书性能探针（诊断工具，只采集数据，不参与业务逻辑）。
 *
 * 输出双通道：logcat tag [TAG] + AppLog（书架「更多 → 日志」面板可直接查看，无需 adb）。
 *
 * - [arm] 开启一个测量窗口（open/close）并启动主线程卡顿看门狗；
 * - [event] 记录相对窗口起点的里程碑时间戳；
 * - 看门狗以 [PROBE_INTERVAL_MS] 周期探测主线程派发延迟，超过 [STALL_THRESHOLD_MS]
 *   时抓取主线程调用栈并输出（带 1.5s 冷却），可直接回答“主线程卡在那 1-3 秒时在干什么”；
 * - 窗口最长 [WINDOW_MAX_MS] 自动结束，避免常驻开销。
 */
object ReaderSwitchPerf {

    private const val TAG = "ReaderSwitchPerf"
    private const val PROBE_INTERVAL_MS = 100L
    private const val STALL_THRESHOLD_MS = 300L
    private const val STALL_CAPTURE_COOLDOWN_MS = 1500L
    private const val WINDOW_MAX_MS = 15_000L
    private const val STACK_FRAME_LIMIT = 30

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var windowStartElapsed = 0L

    @Volatile
    private var windowName = ""

    @Volatile
    private var watchdogActive = false

    @Volatile
    private var lastCaptureAt = 0L

    @Volatile
    private var probeLastScheduledAt = 0L

    private val probe: Runnable = Runnable {
        val now = SystemClock.elapsedRealtime()
        val scheduledAt = probeLastScheduledAt
        if (scheduledAt != 0L) {
            val delay = now - scheduledAt - PROBE_INTERVAL_MS
            if (delay > STALL_THRESHOLD_MS && now - lastCaptureAt >= STALL_CAPTURE_COOLDOWN_MS) {
                lastCaptureAt = now
                reportStall(delay, now)
            }
        }
        if (watchdogActive) {
            probeLastScheduledAt = now
            mainHandler.postDelayed(probe, PROBE_INTERVAL_MS)
        }
    }

    private val autoStopRunnable = Runnable { stopWatchdog("timeout") }

    /** 开启测量窗口；重复调用以最后一次为准（开书后关书各自成窗）。 */
    @Synchronized
    fun arm(window: String) {
        windowName = window
        windowStartElapsed = SystemClock.elapsedRealtime()
        log("$window.begin")
        if (!watchdogActive) {
            watchdogActive = true
            probeLastScheduledAt = 0L
            mainHandler.post(probe)
        }
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.postDelayed(autoStopRunnable, WINDOW_MAX_MS)
    }

    /** 里程碑：相对当前窗口起点的时间戳。从未 arm 过则静默丢弃。 */
    fun event(name: String) {
        val start = windowStartElapsed
        if (start == 0L) return
        log("$name +${SystemClock.elapsedRealtime() - start} ms")
    }

    @Synchronized
    private fun stopWatchdog(reason: String) {
        if (!watchdogActive) return
        watchdogActive = false
        mainHandler.removeCallbacks(probe)
        log("window.$windowName.end($reason)")
    }

    private fun reportStall(delayMs: Long, now: Long) {
        val stack = runCatching {
            Looper.getMainLooper().thread.stackTrace
        }.getOrNull()
            ?.take(STACK_FRAME_LIMIT)
            ?.joinToString("\n") { "      at $it" }
            .orEmpty()
        log("STALL 主线程阻塞约 ${delayMs}ms @$windowName +${now - windowStartElapsed} ms\n$stack")
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        AppLog.putNotSave("[$TAG] $message")
    }
}
