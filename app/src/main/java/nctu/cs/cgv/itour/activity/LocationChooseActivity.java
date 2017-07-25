package nctu.cs.cgv.itour.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.dirPath;

public class LocationChooseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_choose);
    }
}
