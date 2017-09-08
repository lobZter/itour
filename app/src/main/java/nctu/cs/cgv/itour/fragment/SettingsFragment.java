package nctu.cs.cgv.itour.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.LoginActivity;
import nctu.cs.cgv.itour.activity.MainActivity;

import static nctu.cs.cgv.itour.Utility.dpToPx;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;
    private OnFogSwitchedListener onFogSwitchedListener;
    private ActionBar actionBar;

    public interface OnFogSwitchedListener {
        void onFogSwitched();
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        fragment.firebaseAuth = FirebaseAuth.getInstance();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (firebaseAuth.getCurrentUser() == null) {
            getActivity().finish();
            startActivity(new Intent(getContext(), LoginActivity.class));
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference btnSignOut = getPreferenceManager().findPreference("signout");
        btnSignOut.setSummary(firebaseAuth.getCurrentUser().getEmail().toString());
        btnSignOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                firebaseAuth.signOut();
                getActivity().finish();
                startActivity(new Intent(getContext(), LoginActivity.class));
                return true;
            }
        });

        Preference distanceIndicatorSwitch = getPreferenceManager().findPreference("distance_indicator");
        distanceIndicatorSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return true;
            }
        });

        Preference fogSwitch = getPreferenceManager().findPreference("fog");
        fogSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onFogSwitchedListener.onFogSwitched();
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnFogSwitchedListener) {
            onFogSwitchedListener = (OnFogSwitchedListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SettingFragment.OnFogSwitchedListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDivider(null);
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (actionBar != null) {
            if (getUserVisibleHint()) {
                actionBar.setElevation(0);
                actionBar.setSubtitle("Setting");
            } else {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }
}