package sergirex.portadasperiodicos.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sergirex.portadasperiodicos.PRIVACY_POLICY_URL
import sergirex.portadasperiodicos.databinding.DialogConsentBinding
import sergirex.portadasperiodicos.startActivityOrToast

/**
 * The question shown before any ad is requested: may Meta store and read information on the device to measure and
 * personalise ads. It can't be dismissed without answering, and both answers look and weigh the same.
 */
object ConsentDialog {
    fun show(context: Context, onAnswer: (granted: Boolean) -> Unit): AlertDialog {
        val content = DialogConsentBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(content.root)
            .setCancelable(false)
            .create()
        content.privacy.setOnClickListener {
            context.startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
        content.accept.setOnClickListener { dialog.dismiss(); onAnswer(true) }
        content.reject.setOnClickListener { dialog.dismiss(); onAnswer(false) }
        dialog.show()
        return dialog
    }
}
