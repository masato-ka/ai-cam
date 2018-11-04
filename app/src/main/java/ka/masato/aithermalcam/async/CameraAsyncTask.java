package ka.masato.aithermalcam.async;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import ka.masato.aithermalcam.view.DetectionView;

public class CameraAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {

    private static final String TAG = "CameraAsyncTask";
    private DetectionView detectionView;

    public CameraAsyncTask(DetectionView detectionView) {
        this.detectionView = detectionView;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Bitmap doInBackground(Bitmap... bitmap) {
        return bitmap[0];
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        detectionView.setCameraImage(bitmap);
        detectionView.invalidate();
    }
}
