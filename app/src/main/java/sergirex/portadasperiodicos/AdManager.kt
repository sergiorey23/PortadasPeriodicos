package sergirex.portadasperiodicos

import android.content.Context
import android.widget.LinearLayout
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.facebook.ads.AudienceNetworkAds

class AdManager(private val context: Context) {

    private var adView: AdView? = null

    init {
        AudienceNetworkAds.initialize(context)
    }

    fun loadBannerAd(adContainer: LinearLayout) {
        val isPhone = context.resources.getBoolean(R.bool.isPhone)
        val bannerSize = if (isPhone) AdSize.BANNER_HEIGHT_50 else AdSize.BANNER_HEIGHT_90
        adView = AdView(context, "799967435028134_799969321694612", bannerSize).also {
            adContainer.addView(it)
            it.loadAd()
        }
    }

    fun destroy() {
        adView?.destroy()
    }
}
