package sergirex.portadasperiodicos;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String mode = PreferenceManager.getDefaultSharedPreferences(this).getString("theme","default");
        switch (mode) {
            case "default":
                if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    setTheme(R.style.AppThemeDark);
                }
                break;
            case "dark":
                setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

    }

    static public class MyPreferenceFragment extends PreferenceFragmentCompat
    {
        @Override

        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            androidx.preference.ListPreference modesLP = findPreference("theme");
            modesLP.setSummary(modesLP.getEntry());
            modesLP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    preference.setDefaultValue(newValue);
                    Activity activity = getActivity();
                    if(activity != null) {
                        getActivity().recreate();
                    }
                    return true;
                }
            });
            androidx.preference.ListPreference categoriesLP = findPreference("init_category");
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
