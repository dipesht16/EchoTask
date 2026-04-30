package com.example.smartvoicemanager.data.util

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/**
 * Centralized AdMob helper.
 *
 * Uses only AdMob test IDs so banners are safe during development.
 */
object AdUtil {

    // AdMob test banner ad unit id.
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    @Volatile
    private var initialized: Boolean = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            MobileAds.initialize(context) {
                initialized = true
            }
        }
    }

    /**
     * Creates a banner ad view and triggers loading immediately.
     *
     * Callers should destroy the view when no longer needed.
     */
    fun createBannerAdView(context: Context): AdView {
        // Ensure SDK initialization is kicked off early.
        initialize(context.applicationContext)

        return AdView(context.applicationContext).apply {
            adUnitId = TEST_BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
    }
}

