package sergirex.portadasperiodicos

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class BillingManager(
    private val activity: Activity,
    private val prefs: SharedPreferences,
    private val listener: BillingListener?
) {

    interface BillingListener {
        fun onPurchaseAcknowledged()
    }

    private val productDetailsList = mutableListOf<ProductDetails>()

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(::handlePurchases)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        establishConnection()
    }

    private fun handlePurchases(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun establishConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                establishConnection()
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads_id")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { _, result ->
            productDetailsList.clear()
            productDetailsList.addAll(result.productDetailsList)
        }
    }

    fun showPurchaseDialog() {
        val product = productDetailsList.firstOrNull()
        if (product == null) {
            Toast.makeText(activity, "No products available", Toast.LENGTH_LONG).show()
            return
        }
        val price = product.oneTimePurchaseOfferDetails!!.formattedPrice
        MaterialAlertDialogBuilder(activity)
            .setTitle(product.name)
            .setMessage("Deshazte de la publicidad por $price de por vida")
            .setIcon(R.mipmap.news_icon)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Restaurar compra") { _, _ -> restorePurchases() }
            .setPositiveButton("Comprar") { _, _ -> launchPurchaseFlow(product) }
            .show()
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                prefs.edit { putBoolean("remove_fb_ads", true) }
                listener?.onPurchaseAcknowledged()
            }
        }
        Log.d(TAG, "Purchase Token: ${purchase.purchaseToken}")
        Log.d(TAG, "Purchase Time: ${purchase.purchaseTime}")
        Log.d(TAG, "Purchase OrderID: ${purchase.orderId}")
    }

    fun restorePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val snackbar = if (purchases.isNotEmpty()) {
                prefs.edit { putBoolean("remove_fb_ads", true) }
                listener?.onPurchaseAcknowledged()
                Snackbar.make(activity.findViewById(R.id.drawerLayout), "Successfully restored", Snackbar.LENGTH_LONG)
            } else {
                Log.d(TAG, "Oops, No purchase found.")
                Snackbar.make(activity.findViewById(R.id.drawerLayout), "No purchase found", Snackbar.LENGTH_LONG)
            }
            snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE
            snackbar.show()
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
