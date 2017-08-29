package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.Utility.hideSoftKeyboard;

public class PhotoCheckinActivity extends AppCompatActivity {

    private static final String TAG = "PhotoCheckinActivity";
    private String filename = null;
    // view references
    private AutoCompleteTextView locationEdit;
    private EditText descriptionEdit;
    private ImageView pickedPhoto;
    private ImageView cancelBtn;
    private LinearLayout pickPhotoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_checkin);

        // set actionBar title, top-left icon
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        // get view reference
        locationEdit = (AutoCompleteTextView) findViewById(R.id.et_location);
        descriptionEdit = (EditText) findViewById(R.id.et_description);
        pickedPhoto = (ImageView) findViewById(R.id.picked_photo);
        cancelBtn = (ImageView) findViewById(R.id.btn_cancel);
        pickPhotoBtn = (LinearLayout) findViewById(R.id.btn_pick_photo);

        // set location autocomplete
        ArrayList<String> array = new ArrayList<>();
        array.addAll(spotList.getSpots());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_search, array);
        locationEdit.setThreshold(1);
        locationEdit.setAdapter(adapter);

        pickPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(true)
                        .setAspectRatio(1, 1)
                        .start(PhotoCheckinActivity.this);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filename = null;
                pickedPhoto.setVisibility(View.GONE);
                cancelBtn.setVisibility(View.GONE);
                pickPhotoBtn.setVisibility(View.VISIBLE);
            }
        });

        setHideKeyboard(findViewById(R.id.parent_layout));
    }

    public void setHideKeyboard(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(PhotoCheckinActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setHideKeyboard(innerView);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_next, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_next:
                if (filename == null) {
                    Toast.makeText(getApplicationContext(), "請選取圖片", Toast.LENGTH_LONG).show();
                    return true;
                }
                Intent intent = new Intent(PhotoCheckinActivity.this, LocationChooseActivity.class);
                intent.putExtra("location", locationEdit.getText().toString().trim());
                intent.putExtra("description", descriptionEdit.getText().toString().trim());
                intent.putExtra("filename", filename);
                intent.putExtra("type", "photo");
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                String path = result.getUri().getPath();
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                // /data/user/0/nctu.cs.cgv.itour/cache/cropped1795714260.jpg
                // getCacheDir()
                filename = path.substring(path.lastIndexOf("/") + 1);
                pickedPhoto.setImageBitmap(bitmap);
                pickedPhoto.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                pickPhotoBtn.setVisibility(View.GONE);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
