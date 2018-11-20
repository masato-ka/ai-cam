package ka.masato.aithermalcam.async;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TimingLogger;
import ka.masato.aithermalcam.view.DetectionView;
import ka.masato.library.ai.ssddetection.MultiObjectDetector;
import ka.masato.library.ai.ssddetection.exception.UnInitializeDetectorException;
import ka.masato.library.ai.ssddetection.model.Recognition;

import java.util.ArrayList;


public class ObjectDetectionAsyncTask extends AsyncTask<Bitmap, Void, ArrayList<Recognition>> {

    private static final String TAG = "ObjectDetectionAsyncTask";

    private static final int RECOGNITION_IMAGE_WIDTH = 300;
    private static final int RECOGNITION_IMAGE_HEIGHT = 300;

    private MultiObjectDetector multiObjectDetector;
    private DetectionView detectionView;

    public ObjectDetectionAsyncTask(MultiObjectDetector multiObjectDetector, DetectionView detectionView) {
        this.multiObjectDetector = multiObjectDetector;
        this.detectionView = detectionView;
    }


    @Override
    protected ArrayList<Recognition> doInBackground(Bitmap... bitmaps) {
        if (bitmaps[0] == null) {
            return null;
        }
        Bitmap recogBitmap = Bitmap.createScaledBitmap(bitmaps[0],
                RECOGNITION_IMAGE_WIDTH, RECOGNITION_IMAGE_HEIGHT, false);

        ArrayList<Recognition> recognitions = new ArrayList<>();
        try {
            TimingLogger logger = new TimingLogger("LOG_TAG_TEST", "testTimingLogger");
            recognitions = multiObjectDetector.runDetection(recogBitmap, 10);
            detectionView.setDetectionObjects(recognitions);
            logger.addSplit("afterDetection");
            logger.dumpToLog();
            recognitions.stream().map(e -> e.getTitle()).forEach(System.out::println);
        } catch (UnInitializeDetectorException e) {
            Log.d(TAG, "Please initialize detector");
        }
        detectionView.setDetectionImage(bitmaps[0]);
        return recognitions;
    }

    @Override
    protected void onPostExecute(ArrayList<Recognition> recognitions) {
        if (recognitions == null) {
            return;
        }
        detectionView.setDetectionObjects(recognitions);
        //detectionView.invalidate();
    }
}
