package sergirex.portadasperiodicos;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class MyApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate() {
        super.onCreate();
        // Registra un listener para escuchar cambios en las preferencias
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Aplica el tema guardado al iniciar la app
        applyTheme(prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Se llama cuando el usuario cambia el tema en los ajustes
        if ("theme".equals(key)) {
            applyTheme(sharedPreferences);
        }
    }

    private void applyTheme(SharedPreferences sharedPreferences) {
        String themeValue = sharedPreferences.getString("theme", "default"); // "default" es tu valor para "Sistema"
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // "default" o cualquier otro valor
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
    