package com.driftiq.app.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.driftiq.app.data.local.db.entity.AppUsageEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects Android app usage sessions using the UsageEvents API.
 *
 * Uses MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND event pairs to reconstruct
 * individual app sessions with accurate start/end timestamps.
 *
 * queryUsageStats(INTERVAL_BEST) returns cumulative aggregates — it does NOT
 * produce per-session data and its firstTimeStamp/lastTimeStamp fields span
 * the entire aggregation interval, making incremental collection unreliable.
 * UsageEvents.queryEvents() gives us individual lifecycle events that can be
 * correctly windowed by lastCollectionTime.
 */
@Singleton
class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Start slightly in the past on first run to capture any recent sessions
    private var lastCollectionTime: Long = System.currentTimeMillis() - LOOKBACK_MS

    companion object {
        private const val TAG = "UsageStatsCollector"
        private const val LOOKBACK_MS = 30 * 60 * 1000L  // 30 min on first run
        private const val MIN_SESSION_SECONDS = 5

        // System packages to always exclude
        private val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.launcher",
            "com.android.settings",
            "com.android.keyguard",
            "com.android.inputmethod.latin",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.packageinstaller",
            "com.android.vending",
            "com.android.phone",
            "com.driftiq.app",
        )

        val CATEGORY_MAP: Map<String, String> = mapOf(
            // ── Social ─────────────────────────────────────────────
            "com.instagram.android" to "SOCIAL",
            "com.twitter.android" to "SOCIAL",
            "com.facebook.katana" to "SOCIAL",
            "com.facebook.lite" to "SOCIAL",
            "com.snapchat.android" to "SOCIAL",
            "com.reddit.frontpage" to "SOCIAL",
            "com.pinterest" to "SOCIAL",
            "com.tumblr" to "SOCIAL",
            "com.vk.vkontakte" to "SOCIAL",
            "com.zhiliaoapp.musically" to "SOCIAL",
            "com.ss.android.ugc.trill" to "SOCIAL",
            "com.bereal.ft" to "SOCIAL",
            "com.discord" to "SOCIAL",
            "com.threads.app" to "SOCIAL",
            "com.linkedin.android" to "SOCIAL",
            "com.meetup" to "SOCIAL",
            "com.skype.raider" to "SOCIAL",
            "com.clubhouse.clubhouse" to "SOCIAL",

            // ── Communication ───────────────────────────────────────
            "com.whatsapp" to "COMMUNICATION",
            "com.whatsapp.w4b" to "COMMUNICATION",
            "org.telegram.messenger" to "COMMUNICATION",
            "com.viber.voip" to "COMMUNICATION",
            "com.kakao.talk" to "COMMUNICATION",
            "jp.naver.line.android" to "COMMUNICATION",
            "com.microsoft.teams" to "COMMUNICATION",
            "us.zoom.videomeetings" to "COMMUNICATION",
            "com.webex.meetings" to "COMMUNICATION",
            "com.signal.android" to "COMMUNICATION",
            "com.google.android.talk" to "COMMUNICATION",
            "com.google.android.apps.messaging" to "COMMUNICATION",
            "com.android.mms" to "COMMUNICATION",

            // ── Productivity ────────────────────────────────────────
            "com.google.android.gm" to "PRODUCTIVITY",
            "com.microsoft.office.outlook" to "PRODUCTIVITY",
            "com.yahoo.mobile.client.android.mail" to "PRODUCTIVITY",
            "com.google.android.apps.docs" to "PRODUCTIVITY",
            "com.google.android.apps.sheets" to "PRODUCTIVITY",
            "com.google.android.apps.slides" to "PRODUCTIVITY",
            "com.microsoft.office.word" to "PRODUCTIVITY",
            "com.microsoft.office.excel" to "PRODUCTIVITY",
            "com.microsoft.office.powerpoint" to "PRODUCTIVITY",
            "com.microsoft.onenote" to "PRODUCTIVITY",
            "com.google.android.keep" to "PRODUCTIVITY",
            "com.evernote" to "PRODUCTIVITY",
            "com.notion.id" to "PRODUCTIVITY",
            "com.todoist.android.Todoist" to "PRODUCTIVITY",
            "com.asana.app" to "PRODUCTIVITY",
            "com.trello" to "PRODUCTIVITY",
            "com.slack" to "PRODUCTIVITY",
            "com.google.android.apps.tasks" to "PRODUCTIVITY",
            "com.adobe.reader" to "PRODUCTIVITY",
            "md.obsidian" to "PRODUCTIVITY",
            "com.anydo" to "PRODUCTIVITY",
            "com.ticktick.task" to "PRODUCTIVITY",
            "com.basecamp.bc3" to "PRODUCTIVITY",

            // ── Entertainment ───────────────────────────────────────
            "com.netflix.mediaclient" to "ENTERTAINMENT",
            "com.google.android.youtube" to "ENTERTAINMENT",
            "com.google.android.youtube.music" to "ENTERTAINMENT",
            "com.spotify.music" to "ENTERTAINMENT",
            "com.amazon.avod.thirdpartyclient" to "ENTERTAINMENT",
            "com.disney.disneyplus" to "ENTERTAINMENT",
            "com.hbo.hbonow" to "ENTERTAINMENT",
            "com.hulu.plus" to "ENTERTAINMENT",
            "com.crunchyroll.crunchyroid" to "ENTERTAINMENT",
            "tv.twitch.android.app" to "ENTERTAINMENT",
            "com.apple.android.music" to "ENTERTAINMENT",
            "com.soundcloud.android" to "ENTERTAINMENT",
            "com.deezer.android" to "ENTERTAINMENT",
            "com.tidal.app" to "ENTERTAINMENT",
            "com.google.android.videos" to "ENTERTAINMENT",
            "com.amazon.kindle" to "ENTERTAINMENT",
            "com.audible.application" to "ENTERTAINMENT",
            "com.plex.android" to "ENTERTAINMENT",
            "tv.kodi.kodi" to "ENTERTAINMENT",
            "com.vimeo.android.videoapp" to "ENTERTAINMENT",
            "com.espn.score_center" to "ENTERTAINMENT",

            // ── Learning ────────────────────────────────────────────
            "org.duolingo" to "LEARNING",
            "com.udemy.android" to "LEARNING",
            "com.coursera.android" to "LEARNING",
            "com.edx.mobile" to "LEARNING",
            "com.linkedin.learning" to "LEARNING",
            "com.khanacademy.android" to "LEARNING",
            "com.busuu.android" to "LEARNING",
            "com.babbel.mobile.android.en" to "LEARNING",
            "com.skillshare.app" to "LEARNING",
            "com.masterclass.android" to "LEARNING",
            "com.brilliant.android" to "LEARNING",
            "com.chess" to "LEARNING",
            "com.sololearn" to "LEARNING",
            "com.quizlet.quizletandroid" to "LEARNING",
            "com.wattpad.mobile" to "LEARNING",
            "com.blinkslabs.blinkist.android" to "LEARNING",
            "com.wikipedia" to "LEARNING",

            // ── Health ──────────────────────────────────────────────
            "com.google.android.apps.fitness" to "HEALTH",
            "com.fitbit.FitbitMobile" to "HEALTH",
            "com.samsung.android.shealth" to "HEALTH",
            "com.strava" to "HEALTH",
            "com.garmin.android.apps.connectmobile" to "HEALTH",
            "com.nianticlabs.pokemongo" to "HEALTH",
            "com.calm.android" to "HEALTH",
            "com.headspace.android" to "HEALTH",
            "com.cronometer.android" to "HEALTH",
            "com.myfitnesspal.android" to "HEALTH",
            "com.nike.ntc" to "HEALTH",
            "com.runtastic.android" to "HEALTH",
            "com.peloton.android" to "HEALTH",

            // ── Utility ─────────────────────────────────────────────
            "com.google.android.apps.maps" to "UTILITY",
            "com.waze" to "UTILITY",
            "com.ubercab" to "UTILITY",
            "com.lyft.android" to "UTILITY",
            "com.google.android.apps.translate" to "UTILITY",
            "com.paypal.android.p2pmobile" to "UTILITY",
            "com.venmo" to "UTILITY",
            "com.cashapp" to "UTILITY",
            "com.weather.Weather" to "UTILITY",
            "com.google.android.calendar" to "UTILITY",
            "com.google.android.contacts" to "UTILITY",
            "com.google.android.dialer" to "UTILITY",
            "com.android.chrome" to "UTILITY",
            "org.mozilla.firefox" to "UTILITY",
            "com.microsoft.edge" to "UTILITY",
            "com.opera.browser" to "UTILITY",
        )
    }

    /**
     * Collect app usage sessions since the last collection time.
     *
     * Queries individual UsageEvents and pairs MOVE_TO_FOREGROUND with
     * MOVE_TO_BACKGROUND to reconstruct sessions. Sessions without a matching
     * BACKGROUND event (app still open) are skipped — they'll be captured
     * on the next collection cycle when the user leaves the app.
     */
    fun collectRecentEvents(): List<AppUsageEventEntity> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: run {
                Log.w(TAG, "UsageStatsManager not available on this device")
                return emptyList()
            }

        val now = System.currentTimeMillis()
        // Overlap 60s to avoid gaps from clock skew between collection cycles
        val queryStart = lastCollectionTime - 60_000L
        val deviceId = getDeviceId()

        val events = mutableListOf<AppUsageEventEntity>()

        // Track foreground start times per package
        val foregroundStarts = mutableMapOf<String, Long>()

        val usageEvents = usm.queryEvents(queryStart, now)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            val pkg = event.packageName ?: continue
            if (pkg in SYSTEM_PACKAGES) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Record when this app came to foreground
                    foregroundStarts[pkg] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val startTime = foregroundStarts.remove(pkg) ?: continue
                    val endTime = event.timeStamp

                    // Skip sessions that started before our last collection window
                    // (we may have already recorded them)
                    if (endTime <= lastCollectionTime) continue

                    val durationMs = endTime - startTime
                    val durationSeconds = (durationMs / 1000).toInt()

                    if (durationSeconds < MIN_SESSION_SECONDS) continue

                    events.add(
                        AppUsageEventEntity(
                            packageName = pkg,
                            sessionStart = startTime,
                            sessionEnd = endTime,
                            durationSeconds = durationSeconds,
                            category = CATEGORY_MAP[pkg],
                            deviceId = deviceId,
                        )
                    )
                }
            }
        }

        lastCollectionTime = now
        Log.d(TAG, "Collected ${events.size} completed sessions (${foregroundStarts.size} apps still in foreground)")
        return events
    }

    /**
     * Check if the app has the PACKAGE_USAGE_STATS permission.
     * This special permission must be granted in Settings → Digital Wellbeing or
     * Settings → Apps → Special app access → Usage access.
     */
    fun hasUsageStatsPermission(): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val now = System.currentTimeMillis()
        // Query a short recent window — if permission is denied this returns empty
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000, now)
        return stats != null && stats.isNotEmpty()
    }

    private fun getDeviceId(): String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        ) ?: UUID.randomUUID().toString()
}
