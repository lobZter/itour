package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import nctu.cs.cgv.itour.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
    }
}