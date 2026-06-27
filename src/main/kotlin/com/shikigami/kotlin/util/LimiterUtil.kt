package com.shikigami.kotlin.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object LimiterUtil {
    val commandStartLimiter by lazy {
        RateLimiter(1, 60000)
    }

    val commandOpenaiLimiter by lazy {
        RateLimiter(10, 60000)
    }

    class RateLimiter(
        private val maxCount: Int,
        private val windowMillis: Long,
        private val expireMillis: Long = windowMillis
    ) {
        private val limiters = ConcurrentHashMap<String, FixedWindowRateLimiter>()

        fun allow(key: String): Boolean {
            val now = System.currentTimeMillis()

            limiters.entries.removeIf {
                (now - it.value.lastAccessTime) > expireMillis
            }

            val limiter = limiters.computeIfAbsent(key) {
                FixedWindowRateLimiter(maxCount, windowMillis)
            }

            limiter.touch()

            return limiter.allow()
        }

        class FixedWindowRateLimiter(private val maxCount: Int, private val windowMillis: Long) {
            @Volatile
            private var windowStart = System.currentTimeMillis()

            @Volatile
            var lastAccessTime = System.currentTimeMillis()
                private set

            private val counter = AtomicInteger(0)

            fun touch() {
                lastAccessTime = System.currentTimeMillis()
            }

            fun allow(): Boolean {
                val now = System.currentTimeMillis()

                synchronized(this) {
                    if (now - windowStart > windowMillis) {
                        windowStart = now
                        counter.set(0)
                    }

                    return if (counter.get() < maxCount) {
                        counter.incrementAndGet()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}