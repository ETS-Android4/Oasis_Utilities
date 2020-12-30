package com.akapps.oasisutilities.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.view.Display;
import androidx.annotation.RequiresApi;
import com.akapps.oasisutilities.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Camera {
    /**
     * Resamples the captured photo to fit the screen for better memory usage.
     *
     * @param context   The application context.
     * @param imagePath The path of the photo to be resampled.
     * @return The resampled bitmap
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static Bitmap resamplePic(Context context, String imagePath) {
        // Get device screen size information
        Display display = context.getDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int targetW = size.x;
        int targetH = size.y;

        // Get the dimensions of the original bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeFile(imagePath);
    }

    /**
     * Creates the temporary image file in the cache directory.
     *
     * @return The temporary image file.
     * @throws IOException Thrown if there is an error creating the file
     */
    public static File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalCacheDir();

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    /**
     * Deletes image file for a given path.
     *
     * @param context   The application context.
     * @param imagePath The path of the photo to be deleted.
     */
    public static boolean deleteImageFile(Context context, String imagePath) {
        // Get the file
        File imageFile = new File(imagePath);

        // Delete the image
        return imageFile.delete();
    }

    /**
     * Helper method for saving the image.
     *
     * @param context The application context.
     * @param image   The image to be saved.
     * @return The path of the saved image.
     */
    public static String saveImage(Context context, Bitmap image, boolean isCheck) {
        String savedImagePath = null;
        String path = "";

        if(isCheck)
            path = "check_";

        // Create the new file in the external storage
        String imageFileName = path + context.getString(R.string.temp_photo) + ".jpg";
        File storageDir = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        + context.getString(R.string.app_name));
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }

        // Save the new Bitmap
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 20, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return savedImagePath;
    }

}
