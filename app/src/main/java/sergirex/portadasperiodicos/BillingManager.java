package sergirex.portadasperiodicos;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BillingManager {

    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final Activity activity;
    private final SharedPreferences prefs;
    private final List<ProductDetails> productDetailsList = new ArrayList<>();
    private final BillingListener listener;

    public interface BillingListener {
        void onPurchaseAcknowledged();
    }

    public BillingManager(Activity activity, SharedPreferences prefs, BillingListener listener) {
        this.activity = activity;
        this.prefs = prefs;
        this.listener = listener;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this::handlePurchases)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        establishConnection();
    }

    private void handlePurchases(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    private void establishConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                establishConnection();
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> productList = List.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("remove_ads_id")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(
                params,
                (billingResult, prodDetailsList) -> {
                    productDetailsList.clear();
                    productDetailsList.addAll(prodDetailsList.getProductDetailsList());
                }
        );
    }

    public void showPurchaseDialog() {
        if (!productDetailsList.isEmpty()) {
            String price = Objects.requireNonNull(productDetailsList.get(0).getOneTimePurchaseOfferDetails()).getFormattedPrice();
            String productName = productDetailsList.get(0).getName();
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(productName)
                    .setMessage("Deshazte de la publicidad por " + price + " de por vida")
                    .setIcon(R.mipmap.news_icon)
                    .setNegativeButton("Cancelar", null)
                    .setNeutralButton("Restaurar compra", (dialog, which) -> restorePurchases())
                    .setPositiveButton("Comprar", (dialog, which) -> launchPurchaseFlow(productDetailsList.get(0))).show();
        } else {
            Toast.makeText(activity, "No products available", Toast.LENGTH_LONG).show();
        }
    }


    private void launchPurchaseFlow(ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                List.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    private void handlePurchase(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build(), billingResult -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("remove_fb_ads", true);
                    editor.apply();
                    if (listener != null) {
                        listener.onPurchaseAcknowledged();
                    }
                }
            });
            Log.d(TAG, "Purchase Token: " + purchase.getPurchaseToken());
            Log.d(TAG, "Purchase Time: " + purchase.getPurchaseTime());
            Log.d(TAG, "Purchase OrderID: " + purchase.getOrderId());
        }
    }

    public void restorePurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Snackbar sb;
                        if (!list.isEmpty()) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("remove_fb_ads", true);
                            editor.apply();
                            sb = Snackbar.make(activity.findViewById(R.id.drawerLayout), "Successfully restored", Snackbar.LENGTH_LONG);
                            if (listener != null) {
                                listener.onPurchaseAcknowledged();
                            }
                        } else {
                            Log.d(TAG, "Oops, No purchase found.");
                            sb = Snackbar.make(activity.findViewById(R.id.drawerLayout), "No purchase found", Snackbar.LENGTH_LONG);
                        }
                        sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                        sb.show();
                    }
                });
    }

    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}
