package sergirex.portadasperiodicos

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The one-time "remove ads" purchase (Google Play Billing).
 *
 * Ownership is what Google Play says it is: [syncOwnership] runs at start-up and after every
 * purchase/restore, granting the entitlement for a completed purchase of [PRODUCT_ID] and
 * revoking it if the purchase is gone (refund). A failed query never changes anything, so being
 * offline can't take the entitlement away. Pending purchases (slow payment methods) grant
 * nothing until they complete, and every completed purchase is acknowledged (Play refunds
 * unacknowledged purchases after 3 days).
 *
 * The flag is kept in [prefs] under [KEY_ADS_REMOVED]; screens read it with [isAdsRemoved].
 */
class BillingManager(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val prefs: SharedPreferences,
    private val snackbarAnchor: () -> View,
    private val listener: BillingListener?
) {

    interface BillingListener {
        /** The entitlement was just granted (purchase completed or restored). */
        fun onAdsRemoved()
    }

    private enum class SyncResult { OWNED, NOT_OWNED, ERROR }

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener { result, purchases -> scope.launch { onPurchasesUpdated(result, purchases) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var product: ProductDetails? = null

    init {
        scope.launch {
            if (connect()) {
                loadProduct()
                syncOwnership()
            }
        }
    }

    fun showPurchaseDialog() {
        scope.launch {
            val details = product ?: run { loadProduct(); product }
            val price = details?.oneTimePurchaseOfferDetails?.formattedPrice
            if (details == null || price == null) {
                Toast.makeText(activity, R.string.billing_no_products, Toast.LENGTH_LONG).show()
                return@launch
            }
            MaterialAlertDialogBuilder(activity)
                .setTitle(details.name)
                .setMessage(activity.getString(R.string.billing_remove_ads_message, price))
                .setIcon(R.mipmap.news_icon)
                .setNegativeButton(R.string.billing_cancel, null)
                .setNeutralButton(R.string.billing_restore) { _, _ -> restorePurchases() }
                .setPositiveButton(R.string.billing_buy) { _, _ -> launchPurchaseFlow(details) }
                .show()
        }
    }

    fun restorePurchases() {
        scope.launch {
            val message = when (syncOwnership()) {
                SyncResult.OWNED -> R.string.billing_restored
                SyncResult.NOT_OWNED -> R.string.billing_nothing_to_restore
                SyncResult.ERROR -> R.string.billing_error
            }
            Snackbar.make(snackbarAnchor(), message, Snackbar.LENGTH_LONG)
                .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
                .show()
        }
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    private fun launchPurchaseFlow(details: ProductDetails) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            Toast.makeText(activity, R.string.billing_error, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().filter { PRODUCT_ID in it.products }.forEach { purchase ->
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> grantAndAcknowledge(purchase)
                    Purchase.PurchaseState.PENDING ->
                        Toast.makeText(activity, R.string.billing_purchase_pending, Toast.LENGTH_LONG).show()
                    else -> Unit
                }
            }
            // The user already owns it (e.g. bought on another device): just make the app agree.
            BillingResponseCode.ITEM_ALREADY_OWNED -> syncOwnership()
            BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                Log.w(TAG, "Purchase failed: ${result.responseCode} ${result.debugMessage}")
                Toast.makeText(activity, R.string.billing_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Asks Google Play what the user owns and makes the local flag match; see the class comment. */
    private suspend fun syncOwnership(): SyncResult {
        if (!ensureConnected()) return SyncResult.ERROR
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases failed: ${result.billingResult.responseCode}")
            return SyncResult.ERROR
        }
        val owned = result.purchasesList.filter {
            PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        owned.forEach { grantAndAcknowledge(it) }
        if (owned.isEmpty()) setAdsRemoved(false)
        return if (owned.isEmpty()) SyncResult.NOT_OWNED else SyncResult.OWNED
    }

    private suspend fun grantAndAcknowledge(purchase: Purchase) {
        setAdsRemoved(true)
        if (purchase.isAcknowledged) return
        val result = billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        )
        // A failure is retried by the next syncOwnership() (it sees the purchase still unacknowledged).
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "acknowledge failed: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private fun setAdsRemoved(removed: Boolean) {
        val changed = prefs.getBoolean(KEY_ADS_REMOVED, false) != removed
        prefs.edit { putBoolean(KEY_ADS_REMOVED, removed) }
        if (changed && removed) listener?.onAdsRemoved()
    }

    private suspend fun loadProduct() {
        if (!ensureConnected()) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            product = result.productDetailsList?.firstOrNull()
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.responseCode}")
        }
    }

    private suspend fun ensureConnected(): Boolean = billingClient.isReady || connect()

    private suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
            }

            // Auto service reconnection is enabled on the client, so there's nothing to do here.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    companion object {
        private const val TAG = "BillingManager"
        private const val PRODUCT_ID = "remove_ads_id"
        private const val KEY_ADS_REMOVED = "remove_fb_ads"

        fun isAdsRemoved(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_ADS_REMOVED, false)
    }
}
