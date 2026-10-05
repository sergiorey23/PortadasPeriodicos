package sergirex.portadasperiodicos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.usecase.GetFavoritePeriodicosUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPortadaCoversUseCase
import javax.inject.Inject

/**
 * Despite the name, the old FavoritosWidget.java never actually read the
 * user's favorites: it carried its own separate hardcoded ~37-entry
 * newspaper list (not the categorized catalog, not real favorites — a third,
 * independent copy of "which newspapers exist") that left/right cycled
 * through regardless of what the user had actually favorited. That's fixed
 * here: the widget now reads the same FavoritePeriodicosRepository the
 * Favoritos tab uses, and shows an explicit empty state when there are none,
 * instead of silently cycling through newspapers the user never chose.
 *
 * Also fixed: the buttons' PendingIntents were built with
 * PendingIntent.getService(), targeting this class as if it were a Service —
 * it's a BroadcastReceiver (AppWidgetProvider IS-A BroadcastReceiver), so no
 * Service with that component exists and tapping a button would fail
 * silently. getBroadcast() is correct. There were also two different
 * "is it still yesterday's edition" hour thresholds (6am in one method, 4am
 * in another) for what's meant to be the same notion of "today" — unified
 * on PortadasUtils.effectiveTodayDate(), the same one every other screen uses.
 *
 * Threading-wise, the three `new Thread { ... }` blocks (one in onUpdate, one
 * per button in onReceive) are replaced with goAsync() + a coroutine: a
 * BroadcastReceiver's process can be killed the moment onReceive() returns
 * unless goAsync()'s PendingResult is still open, which a bare background
 * Thread does nothing to prevent.
 */
@AndroidEntryPoint
class FavoritosWidget : AppWidgetProvider() {

    @Inject
    lateinit var getFavoritePeriodicos: GetFavoritePeriodicosUseCase

    @Inject
    lateinit var getPortadaCovers: GetPortadaCoversUseCase

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            goAsync { renderWidget(context, appWidgetManager, appWidgetId) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val delta = when (intent.action) {
            ACTION_LEFT -> -1
            ACTION_RIGHT -> 1
            else -> return
        }
        goAsync {
            val favorites = getFavoritePeriodicos()
            if (favorites.isEmpty()) return@goAsync
            val prefs = context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
            val newIndex = (prefs.getInt(KEY_INDEX, 0) + delta).coerceIn(0, favorites.lastIndex)
            prefs.edit { putInt(KEY_INDEX, newIndex) }
            renderWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    private suspend fun renderWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.favoritos_widget)
        val favorites = getFavoritePeriodicos()

        if (favorites.isEmpty()) {
            views.setViewVisibility(R.id.emptyStateText, View.VISIBLE)
            views.setViewVisibility(R.id.imageViewWidget, View.GONE)
            views.setViewVisibility(R.id.leftBtn, View.GONE)
            views.setViewVisibility(R.id.rightBtn, View.GONE)
        } else {
            val prefs = context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
            val index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, favorites.lastIndex)

            views.setViewVisibility(R.id.emptyStateText, View.GONE)
            views.setViewVisibility(R.id.imageViewWidget, View.VISIBLE)
            views.setViewVisibility(R.id.leftBtn, if (index > 0) View.VISIBLE else View.INVISIBLE)
            views.setViewVisibility(R.id.rightBtn, if (index < favorites.lastIndex) View.VISIBLE else View.INVISIBLE)
            views.setOnClickPendingIntent(R.id.leftBtn, navigatePendingIntent(context, appWidgetId, ACTION_LEFT))
            views.setOnClickPendingIntent(R.id.rightBtn, navigatePendingIntent(context, appWidgetId, ACTION_RIGHT))

            loadCoverBitmap(context, favorites[index])?.let { views.setImageViewBitmap(R.id.imageViewWidget, it) }
        }

        views.setOnClickPendingIntent(R.id.imageViewWidget, openAppPendingIntent(context))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /** Reuses PortadaCoverRepository (date resolution) + Coil (fetch/decode/cache) — the same pipeline the list screens use. */
    private suspend fun loadCoverBitmap(context: Context, periodico: PeriodicoRef): Bitmap? {
        val cover = getPortadaCovers(listOf(periodico), PortadasUtils.effectiveTodayDate(), CACHE_GROUP, forceRefresh = false)
            .firstOrNull() ?: return null
        val request = ImageRequest.Builder(context).data(cover.imageUrl).build()
        val result = context.imageLoader.execute(request)
        return ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
    }

    private fun navigatePendingIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, FavoritosWidget::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        // requestCode must be unique per (widget, action) pair or the two PendingIntents collide.
        val requestCode = appWidgetId * 10 + action.hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, Portadas::class.java), PendingIntent.FLAG_IMMUTABLE)

    /** Keeps the receiver's process alive for [block]'s duration, same purpose the old `new Thread{}.start()` calls were missing. */
    private fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val INDEX_PREFS = "IndiceWDT"
        const val KEY_INDEX = "index"
        const val ACTION_LEFT = "android.appwidget.action.LEFT"
        const val ACTION_RIGHT = "android.appwidget.action.RIGHT"
        const val CACHE_GROUP = "Widget"
    }
}
