package nctu.cs.cgv.itour;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.ncapdevi.fragnav.FragNavController;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

public class MainActivity extends AppCompatActivity implements FragNavController.RootFragmentListener {

    private static final String TAG = "MainActivity";
    private BottomBar bottomBar;
    private FragNavController fragNavController;
    private final int INDEX_MAP = FragNavController.TAB1;
    private final int INDEX_PLAN = FragNavController.TAB2;
    private final int INDEX_SETTINGS = FragNavController.TAB3;

    public String mapTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
//        mapTag = intent.getStringExtra("mapTag");
        mapTag = "nctu";

        fragNavController = FragNavController.newBuilder(savedInstanceState, getSupportFragmentManager(), R.id.fragment_content)
                .rootFragmentListener(this, 3)
                .build();

        bottomBar = (BottomBar) findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_map:
                        fragNavController.switchTab(INDEX_MAP);
                        break;
                    case R.id.tab_plan:
                        fragNavController.switchTab(INDEX_PLAN);
                        break;
                    case R.id.tab_settings:
                        fragNavController.switchTab(INDEX_SETTINGS);
                        break;
                }
            }
        });

        bottomBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                fragNavController.clearStack();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!fragNavController.isRootFragment()) {
            fragNavController.popFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (fragNavController != null) {
            fragNavController.onSaveInstanceState(outState);
        }
    }

    @Override
    public Fragment getRootFragment(int index) {
        switch (index) {
            case INDEX_MAP:
                return MapFragment.newInstance(mapTag);
            case INDEX_PLAN:
                return PlanFragment.newInstance();
            case INDEX_SETTINGS:
                return SettingsFragment.newInstance();
        }
        throw new IllegalStateException("Unexpected tab index");
    }
}
