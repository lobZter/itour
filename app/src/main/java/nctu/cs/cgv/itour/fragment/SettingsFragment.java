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

public class SettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;
    private OnFogListener onFogListener;
    private OnDistanceIndicatorListener onDistanceIndicatorListener;
    private OnCheckinIconListener onCheckinIconListener;
    private OnSpotIonListener onSpotIonListener;
    private ActionBar actionBar;

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

        Preference btnSignOut = getPreferenceManager().findPreference("logout");
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

        Preference checkinSwitch = getPreferenceManager().findPreference("checkin");
        checkinSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                onCheckinIconListener.onCheckinIconSwitched((Boolean) newValue);
                return true;
            }
        });

//        Preference spotSwitch = getPreferenceManager().findPreference("spot");
//        spotSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                onSpotIonListener.onSpotIconSwitched((Boolean) newValue);
//                return true;
//            }
//        });
//
//        Preference distanceIndicatorSwitch = getPreferenceManager().findPreference("distance_indicator");
//        distanceIndicatorSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                onDistanceIndicatorListener.onDistanceIndicatorSwitched((Boolean) newValue);
//                return true;
//            }
//        });
//
//        Preference fogSwitch = getPreferenceManager().findPreference("fog");
//        fogSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                onFogListener.onFogSwitched((Boolean) newValue);
//                return true;
//            }
//        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onFogListener = (OnFogListener) context;
            onDistanceIndicatorListener = (OnDistanceIndicatorListener) context;
            onCheckinIconListener = (OnCheckinIconListener) context;
            onSpotIonListener = (OnSpotIonListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement interfaces");
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
                actionBar.setSubtitle(getString(R.string.subtitle_setting));
            }
        }
    }

    public interface OnFogListener {
        void onFogSwitched(boolean flag);
    }

    public interface OnDistanceIndicatorListener {
        void onDistanceIndicatorSwitched(boolean flag);
    }

    public interface OnCheckinIconListener {
        void onCheckinIconSwitched(boolean flag);
    }

    public interface OnSpotIonListener {
        void onSpotIconSwitched(boolean flag);
    }
}