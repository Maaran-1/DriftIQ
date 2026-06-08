package com.driftiq.app.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.driftiq.app.data.local.db.entity.AppUsageEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects Android UsageStats and classifies app sessions into behavioral categories.
 *
 * Category classification uses a hardcoded map for offline support.
 * The backend's ML pipeline uses the category field for feature extraction:
 *   SOCIAL, PRODUCTIVITY, ENTERTAINMENT, LEARNING, HEALTH, UTILITY, COMMUNICATION
 */
@Singleton
class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var lastCollectionTime: Long = System.currentTimeMillis() - LOOKBACK_MS

    companion object {
        private const val TAG = "UsageStatsCollector"
        private const val LOOKBACK_MS = 15 * 60 * 1000L
        private const val MIN_SESSION_SECONDS = 5

        // System packages to always exclude from behavioral data
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
            "com.google.android.gms.persistent",
            "com.android.packageinstaller",
            "com.android.vending",      // Play Store system background
            "com.android.phone",
        )

        /**
         * App package → behavioral category mapping.
         * Covers the top 200 Android apps by install count across key categories.
         * Apps not in this map are tagged as null (backend defaults to UTILITY).
         */
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
            "com.zhiliaoapp.musically" to "SOCIAL",     // TikTok
            "com.ss.android.ugc.trill" to "SOCIAL",     // TikTok (alt)
            "com.bereal.ft" to "SOCIAL",
            "com.discord" to "SOCIAL",
            "com.threads.app" to "SOCIAL",
            "com.linkedin.android" to "SOCIAL",
            "com.meetup" to "SOCIAL",
            "com.skype.raider" to "SOCIAL",             // More social than productivity in practice
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
            "com.google.android.talk" to "COMMUNICATION",  // Google Messages
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
            "com.amazon.avod.thirdpartyclient" to "ENTERTAINMENT",  // Prime Video
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
            "air.tv.ouya.discover" to "ENTERTAINMENT",

            // ── Learning ────────────────────────────────────────────
            "org.duolingo" to "LEARNING",
            "com.udemy.android" to "LEARNING",
            "com.coursera.android" to "LEARNING",
            "com.edx.mobile" to "LEARNING",
            "com.linkedin.learning" to "LEARNING",
            "com.khanacademy.android" to "LEARNING",
            "com.busuu.android" to "LEARNING",
            "com.babbel.mobile.android.en" to "LEARNING",
            "com.pocketpills" to "LEARNING",
            "com.skillshare.app" to "LEARNING",
            "com.masterclass.android" to "LEARNING",
            "com.brilliant.android" to "LEARNING",
            "com.chess" to "LEARNING",
            "com.sololearn" to "LEARNING",
            "com.quizlet.quizletandroid" to "LEARNING",
            "com.readwhere.reader" to "LEARNING",
            "com.wattpad.mobile" to "LEARNING",
            "com.blinkslabs.blinkist.android" to "LEARNING",
            "com.wikipedia" to "LEARNING",

            // ── Health ──────────────────────────────────────────────
            "com.google.android.apps.fitness" to "HEALTH",
            "com.fitbit.FitbitMobile" to "HEALTH",
            "com.stryd.app" to "HEALTH",
            "com.samsung.android.shealth" to "HEALTH",
            "com.strava" to "HEALTH",
            "com.garmin.android.apps.connectmobile" to "HEALTH",
            "com.nianticlabs.pokemongo" to "HEALTH",   // Walking game
            "com.calm.android" to "HEALTH",
            "com.headspace.android" to "HEALTH",
            "com.insight.app" to "HEALTH",
            "com.cronometer.android" to "HEALTH",
            "com.myfitnesspal.android" to "HEALTH",
            "com.nike.ntc" to "HEALTH",
            "com.lego.minifigures" to "HEALTH",
            "com.runtastic.android" to "HEALTH",
            "com.peloton.android" to "HEALTH",

            // ── Utility ─────────────────────────────────────────────
            "com.google.android.apps.maps" to "UTILITY",
            "com.waze" to "UTILITY",
            "com.uber.driver" to "UTILITY",
            "com.ubercab" to "UTILITY",
            "com.lyft.android" to "UTILITY",
            "com.google.android.apps.translate" to "UTILITY",
            "com.paypal.android.p2pmobile" to "UTILITY",
            "com.venmo" to "UTILITY",
            "com.cashapp" to "UTILITY",
            "com.intuit.turbotax.mobile" to "UTILITY",
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
     * Collect usage stats since the last collection time.
     * Filters out system packages and very short sessions (<5 sec).
     */
    fun collectRecentEvents(): List<AppUsageEventEntity> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: run {
                Log.w(TAG, "UsageStatsManager not available on this device")
                return emptyList()
            }

        val now = System.currentTimeMillis()

        // Slight overlap to avoid gaps between collection windows
        val queryStart = lastCollectionTime - 60_000
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, queryStart, now)
            ?: return emptyList()

        val deviceId = getDeviceId()
        val events = mutableListOf<AppUsageEventEntity>()

        for (stat in stats) {
            if (stat.packageName in SYSTEM_PACKAGES) continue
            if (stat.totalTimeInForeground <= 0) continue

            val durationSeconds = (stat.totalTimeInForeground / 1000).toInt()
            if (durationSeconds < MIN_SESSION_SECONDS) continue

            val sessionStart = stat.firstTimeStamp
            val sessionEnd = stat.lastTimeStamp

            // Skip sessions entirely before last collection
            if (sessionEnd < lastCollectionTime) continue

            events.add(
                AppUsageEventEntity(
                    packageName = stat.packageName,
                    sessionStart = sessionStart,
                    sessionEnd = sessionEnd,
                    durationSeconds = durationSeconds,
                    category = CATEGORY_MAP[stat.packageName],
                    deviceId = deviceId,
                )
            )
        }

        lastCollectionTime = now
        Log.d(TAG, "Collected ${events.size} events from ${stats.size} stat records")
        return events
    }

    /**
     * Check if the app has usage stats permission.
     * This is a special Android permission that must be granted in system settings.
     */
    fun hasUsageStatsPermission(): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000, now)
        return stats != null && stats.isNotEmpty()
    }

    private fun getDeviceId(): String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        ) ?: UUID.randomUUID().toString()
}
