package sergirex.portadasperiodicos;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("switch_preference", false))
            setTheme(R.style.Theme_AppCompat_NoActionBar);
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

    }

    static public class MyPreferenceFragment extends PreferenceFragmentCompat
    {
        @Override

        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            SwitchPreferenceCompat theme_switcher = (SwitchPreferenceCompat) findPreference("switch_preference");

            theme_switcher.setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(androidx.preference.Preference preference, Object newValue) {
                    Activity activity = getActivity();
                    if(activity != null) {
                        getActivity().recreate();
                    }
                    return true;
                }
            });
            androidx.preference.ListPreference categoriesLP = (androidx.preference.ListPreference) findPreference("init_category");
            categoriesLP.setSummary(categoriesLP.getValue());
            categoriesLP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    preference.setDefaultValue(newValue);
                    return true;
                }
            });
        }
    }
}
