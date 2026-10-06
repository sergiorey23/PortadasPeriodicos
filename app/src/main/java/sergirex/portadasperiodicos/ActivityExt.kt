package sergirex.portadasperiodicos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Starts [intent] for links and chooser sheets, where the device may have no app able to handle it
 * (no browser, no mail app, no Play Store): the user gets a message instead of a crash.
 */
fun Context.startActivityOrToast(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (error: ActivityNotFoundException) {
    Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show()
    false
}
