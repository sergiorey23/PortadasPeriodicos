package sergirex.portadasperiodicos;

import android.content.Context;
import android.widget.LinearLayout;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;

public class AdManager {

    private final Context context;
    private AdView adView;

    public AdManager(Context context) {
        this.context = context;
        AudienceNetworkAds.initialize(context);
    }

    public void loadBannerAd(LinearLayout adContainer) {
        boolean isPhone = context.getResources().getBoolean(R.bool.isPhone);
        if (isPhone) {
            adView = new AdView(context, "799967435028134_799969321694612", AdSize.BANNER_HEIGHT_50);
        } else {
            adView = new AdView(context, "799967435028134_799969321694612", AdSize.BANNER_HEIGHT_90);
        }
        adContainer.addView(adView);
        adView.loadAd();
    }

    public void destroy() {
        if (adView != null) {
            adView.destroy();
        }
    }
}
