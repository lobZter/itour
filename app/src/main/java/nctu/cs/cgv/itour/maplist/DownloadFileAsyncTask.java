package nctu.cs.cgv.itour.maplist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;

import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.fileServerURL;

/**
 * Created by lobZter on 2017/6/21.
 */

public class DownloadFileAsyncTask extends AsyncTask<String, String, String> {

    private static final String TAG = "DownloadFileAsyncTask";
    private String mapTag;
    private ProgressDialog progressDialog;
    private Context context;

    public DownloadFileAsyncTask(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.dialog_download_file));
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... args) {

        mapTag = args[0];
        ArrayList<Integer> fileLength = new ArrayList<>();
        ArrayList<InputStream> inputStreams = new ArrayList<>();
        ArrayList<FileOutputStream> fileOutputStreams = new ArrayList<>();

        // Make a directory if needed.
        File iTourDir = new File(dirPath);
        iTourDir.mkdirs();

        Log.d(TAG, String.valueOf(Calendar.getInstance().getTime()));

        try {
            URL[] urls = {
                    new URL(fileServerURL + "/" + mapTag + "_distorted_map.png"),
                    new URL(fileServerURL + "/" + mapTag + "_mesh.txt"),
                    new URL(fileServerURL + "/" + mapTag + "_warpMesh.txt"),
                    new URL(fileServerURL + "/" + mapTag + "_bound_box.txt"),
                    new URL(fileServerURL + "/" + mapTag + "_edge_length.txt"),
                    new URL(fileServerURL + "/" + mapTag + "_spot_list.txt")};

            for (URL url : urls) {
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
                // Get file length.
                fileLength.add(urlConnection.getContentLength());
                // Input stream to read file - with 8k buffer
                inputStreams.add(new BufferedInputStream(url.openStream(), 8192));
            }

            // Create a File object for the output file.
            // And attach the OutputStream to the file object,
            // instead of a String representation.
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_distorted_map.png")
            ));
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_mesh.txt")
            ));
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_warpMesh.txt")
            ));
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_bound_box.txt")
            ));
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_edge_length.txt")
            ));
            fileOutputStreams.add(new FileOutputStream(
                    new File(iTourDir + "/" + mapTag + "_spot_list.txt")
            ));


            // Write files and set progress.
            for (int i = 0; i < 6; i++) {
                byte data[] = new byte[1024];
                int lengthTotal = 0;
                int lengthCount;

                while ((lengthCount = inputStreams.get(i).read(data)) != -1) {
                    fileOutputStreams.get(i).write(data, 0, lengthCount);

                    lengthTotal += lengthCount;
                    publishProgress("" + (lengthTotal * 100.0 / fileLength.get(0)));
                }

                fileOutputStreams.get(i).close();
                inputStreams.get(i).close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        return null;
    }

    protected void onProgressUpdate(String... progress) {
        progressDialog.setProgress((int) Float.parseFloat(progress[0]));
    }

    @Override
    protected void onPostExecute(String unused) {
        progressDialog.dismiss();
        context.startActivity(new Intent(context, MainActivity.class));
    }

}