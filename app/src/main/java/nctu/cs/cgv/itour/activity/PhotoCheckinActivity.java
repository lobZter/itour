package nctu.cs.cgv.itour.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.fileUploadURL;
import static nctu.cs.cgv.itour.MyApplication.photoPath;

public class PhotoCheckinActivity extends AppCompatActivity {

    private static final String TAG = "PhotoCheckinActivity";
    private String mapTag;
    private float latitude = 0;
    private float longitude = 0;
    private String filename = " ";
    // pick image
    private static final int PICK_PHOTO_FOR_AVATAR = 1024;
    // view objects
    private EditText locationEdit;
    private EditText descriptionEdit;
    private ImageButton recordBtn;
    private Button submitBtn;
    private ProgressBar progressBar;
    private ImageView pickedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Intent intent = getIntent();
        latitude = intent.getFloatExtra("lat", 0);
        longitude = intent.getFloatExtra("lng", 0);
        mapTag = intent.getStringExtra("mapTag");

        setView();
    }

    private void setView() {

        locationEdit = (EditText) findViewById(R.id.et_location);
        descriptionEdit = (EditText) findViewById(R.id.et_description);
        recordBtn = (ImageButton) findViewById(R.id.btn_record);
        submitBtn = (Button) findViewById(R.id.btn_submit);
        progressBar = (ProgressBar) findViewById(R.id.loading_circle);
        pickedImage = (ImageView) findViewById(R.id.picked_image);

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.setForceMultipartEntityContentType(true);
                try {
                    params.put("file", new File(filename));
                    params.put("lat", latitude);
                    params.put("lng", longitude);
                    params.put("type", "photo");

                    client.post(fileUploadURL, params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onStart() {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            progressBar.setVisibility(View.GONE);
                            finish();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(PhotoCheckinActivity.this, "網路錯誤QQ", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (FileNotFoundException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO_FOR_AVATAR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_FOR_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                pickedImage.setImageBitmap(bitmap);

                // save file
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);

                filename = photoPath + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                File targetFile = new File(filename);
                OutputStream outStream = new FileOutputStream(targetFile);
                outStream.write(buffer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
