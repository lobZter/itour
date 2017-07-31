package nctu.cs.cgv.itour.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.google.firebase.auth.FirebaseAuth;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.LoginActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() == null) {
            getActivity().finish();
            startActivity(new Intent(getContext(), LoginActivity.class));
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference btnSignOut = getPreferenceManager().findPreference("signout");
        btnSignOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    firebaseAuth.signOut();
                    getActivity().finish();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                    return true;
                }
        });
    }
}