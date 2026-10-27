package sergirex.portadasperiodicos

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.ViewGroup
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.AdSettings
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener

/**
 * Meta Audience Network ads, shared by every screen that shows one. One instance per Activity: it owns the [AdView]
 * banner and the [InterstitialAd] it creates and releases them in [destroy].
 */
class AdManager(private val context: Context) {

    private var adView: AdView? = null

    init {
        initializeSdk(context)
    }

    /** Adds a 50dp-tall banner (the smallest size) for [placementId] to [container] and requests an ad. */
    fun loadBanner(container: ViewGroup, placementId: String) {
        val banner = AdView(context, placementId, AdSize.BANNER_HEIGHT_50)
        adView = banner
        container.removeAllViews()
        container.addView(banner)

        val listener = object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                Log.d(TAG, "Banner loaded ($placementId)")
            }

            override fun onError(ad: Ad, adError: AdError) {
                Log.w(TAG, "Banner failed ($placementId): ${adError.errorCode} ${adError.errorMessage}")
            }

            override fun onAdClicked(ad: Ad) = Unit
            override fun onLoggingImpression(ad: Ad) = Unit
        }
        banner.loadAd(banner.buildLoadAdConfig().withAdListener(listener).build())
    }

    private var interstitial: InterstitialAd? = null
    private var onInterstitialClosed: (() -> Unit)? = null

    /** Requests an interstitial for [placementId]; [onLoaded] runs once it can be shown. */
    fun loadInterstitial(placementId: String, onLoaded: () -> Unit) {
        interstitial?.destroy()
        val ad = InterstitialAd(context, placementId)
        interstitial = ad
        val listener = object : InterstitialAdListener {
            override fun onAdLoaded(ad: Ad) {
                Log.d(TAG, "Interstitial loaded ($placementId)")
                onLoaded()
            }

            override fun onError(ad: Ad, adError: AdError) {
                Log.w(TAG, "Interstitial failed ($placementId): ${adError.errorCode} ${adError.errorMessage}")
                // An error while it is on screen must not leave the user stuck behind it.
                closeInterstitial()
            }

            override fun onInterstitialDisplayed(ad: Ad) = Unit
            override fun onInterstitialDismissed(ad: Ad) = closeInterstitial()
            override fun onAdClicked(ad: Ad) = Unit
            override fun onLoggingImpression(ad: Ad) = Unit
        }
        ad.loadAd(ad.buildLoadAdConfig().withAdListener(listener).build())
    }

    val isInterstitialReady: Boolean
        get() = interstitial?.let { it.isAdLoaded && !it.isAdInvalidated } == true

    /** Shows the loaded interstitial and calls [onClosed] when it goes away; returns false if there is none to show. */
    fun showInterstitial(onClosed: () -> Unit): Boolean {
        val ad = interstitial?.takeIf { isInterstitialReady } ?: return false
        onInterstitialClosed = onClosed
        if (ad.show()) return true
        onInterstitialClosed = null
        return false
    }

    private fun closeInterstitial() {
        val callback = onInterstitialClosed ?: return
        onInterstitialClosed = null
        callback()
    }

    fun destroy() {
        adView?.destroy()
        adView = null
        interstitial?.destroy()
        interstitial = null
        onInterstitialClosed = null
    }

    private companion object {
        const val TAG = "AdManager"

        fun initializeSdk(context: Context) {
            val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            // Debug builds only: otherwise Meta treats developer impressions/clicks as invalid traffic.
            if (isDebuggable) AdSettings.setTestMode(true)
            if (AudienceNetworkAds.isInitialized(context)) return
            AudienceNetworkAds.buildInitSettings(context)
                .withInitListener { result -> Log.d(TAG, "Audience Network init: ${result.isSuccess} ${result.message}") }
                .initialize()
        }
    }
}
