package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.photoPath;

public class PhotoCheckinActivity extends AppCompatActivity {

    private static final String TAG = "PhotoCheckinActivity";
    private float lat = 0;
    private float lng = 0;
    private String filename = " ";
    // view objects
    private EditText locationEdit;
    private EditText descriptionEdit;
    private ImageView pickedPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_checkin);

        // get information from previous activity
        Intent intent = getIntent();
        lat = intent.getFloatExtra("lat", 0);
        lng = intent.getFloatExtra("lng", 0);

        setView();

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setFixAspectRatio(true)
                .setAspectRatio(1, 1)
                .start(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu_search; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_submit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_submit:
                String location = locationEdit.getText().toString().trim();
                String description = descriptionEdit.getText().toString().trim();

                Intent intent = new Intent(PhotoCheckinActivity.this, LocationChooseActivity.class);
                intent.putExtra("mapTag", mapTag);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("location", location);
                intent.putExtra("description", description);
                intent.putExtra("filename", filename);
                intent.putExtra("type", "photo");
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setView() {
        locationEdit = (EditText) findViewById(R.id.et_location);
        descriptionEdit = (EditText) findViewById(R.id.et_description);
        pickedPhoto = (ImageView) findViewById(R.id.picked_photo);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                filename = resultUri.getPath();
                Bitmap bitmap = BitmapFactory.decodeFile(resultUri.getPath());
                pickedPhoto.setImageBitmap(bitmap);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
