package nctu.cs.cgv.itour.custom;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.view.Display;
import android.view.Surface;

import com.google.firebase.auth.FirebaseAuth;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import nctu.cs.cgv.itour.service.ScreenShotService;

import static nctu.cs.cgv.itour.MyApplication.imageLogPath;

/**
 * Created by lobst3rd on 2017/11/6.
 */

public class MyImageReader implements ImageReader.OnImageAvailableListener {
    private final int width;
    private final int height;
    private final ImageReader imageReader;
    private final ScreenShotService screenShotService;

    public MyImageReader(ScreenShotService screenShotService) {
        this.screenShotService = screenShotService;

        Display display = screenShotService.getWindowManager().getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);

        int width = size.x;
        int height = size.y;

        while (width * height > (2 << 19)) {
            width = width >> 1;
            height = height >> 1;
        }

        this.width = width;
        this.height = height;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        imageReader.setOnImageAvailableListener(this, screenShotService.getHandler());
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image = imageReader.acquireLatestImage();
        FileOutputStream fileOutputStream = null;
        Bitmap bitmap = null;

        if(FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        try {
            String filename = FirebaseAuth.getInstance().getCurrentUser().getUid() + "-";
            filename += new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            filename += ".jpg";
            String filePath = imageLogPath + "/" + filename;
            fileOutputStream = new FileOutputStream(filePath);
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int bitmapWidth = width + rowPadding / pixelStride;
            bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            screenShotService.screenShotLog(filePath);
            screenShotService.stopCapture();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            if (bitmap != null)
                bitmap.recycle();

            if (image != null)
                image.close();
        }
    }

    public Surface getSurface() {
        return (imageReader.getSurface());
    }

    public int getWidth() {
        return (width);
    }

    public int getHeight() {
        return (height);
    }

    public void close() {
        imageReader.close();
    }
}
