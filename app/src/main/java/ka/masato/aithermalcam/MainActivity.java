package ka.masato.aithermalcam;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.things.pio.PeripheralManager;
import ka.masato.aithermalcam.async.CameraAsyncTask;
import ka.masato.aithermalcam.async.GridEyeAsyncTask;
import ka.masato.aithermalcam.model.ObjectInfo;
import ka.masato.aithermalcam.view.DetectionView;
import ka.masato.grideyelib.driver.GridEyeDriver;
import ka.masato.library.ai.ssddetection.MultiObjectDetector;
import ka.masato.library.ai.ssddetection.exception.FailedInitializeDetectorException;
import ka.masato.library.ai.ssddetection.exception.UnInitializeDetectorException;
import ka.masato.library.ai.ssddetection.model.Recognition;
import ka.masato.library.device.camera.CameraController;
import ka.masato.library.device.camera.ImagePreprocessor;
import ka.masato.library.device.camera.exception.FailedCaptureImageException;
import ka.masato.library.device.camera.exception.NoCameraFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 480;

    private static final String MODEL_FILE_PATH = "detect.tflite";
    private static final String LABEL_FILE_PATH = "labelmap.txt";

    private static final String I2C_PORT = "I2C1";
    private static final int ADDRESS_GRID_EYE = 0x68;

    private DetectionView detectionView;
    private Handler mainHandler;

    private GridEyeDriver gridEyeDriver;
    private Handler gridEyeHandler;


    private ImagePreprocessor preprocessor;
    private CameraController cameraController;
    private Handler cameraSensingHandler;


    private MultiObjectDetector multiObjectDetector;
    private Handler detectionHandler;

    TimingLogger logger = new TimingLogger(TAG + "_TIME", "testTimingLogger");
    private TextView stdLabel;
    private TextView averageTemp;
    private Bitmap showBitmap;
    private boolean isDoObjectDetection = false;
    private boolean doDetection = false;
    private int RECOGNITION_IMAGE_WIDTH = 300;
    private int RECOGNITION_IMAGE_HEIGHT = 300;
    private ArrayList<Recognition> recognitions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Base Layout
        LinearLayout baseLayout = new LinearLayout(this);
        baseLayout.setOrientation(LinearLayout.HORIZONTAL);
        baseLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(baseLayout);

        //detectionView on Base Layout
        detectionView = new DetectionView(this);
        LinearLayout.LayoutParams detectionViewLayoutParam = new LinearLayout.LayoutParams(
                800,
                480
        );
        detectionView.setLayoutParams(detectionViewLayoutParam);

        baseLayout.addView(detectionView);

        //start initialize peripheral
        initializeFIRSensor();
        preprocessor = new ImagePreprocessor(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        initializeCamera();

        initializeMultiObjectDetector();
        HandlerThread detectionThread = new HandlerThread("detection");
        detectionThread.start();
        detectionHandler = new Handler(detectionThread.getLooper());

        HandlerThread mainThread = new HandlerThread("main");
        mainThread.start();
        cameraSensingHandler = new Handler(mainThread.getLooper());
        cameraSensingHandler.post(cameraThread);

        mainHandler = new Handler();


        //HandlerThread handlerThread = new HandlerThread("grideye");
        //handlerThread.start();
        //gridEyeHandler = new Handler(handlerThread.getLooper());
        //gridEyeHandler.post(gridEyeThread);



    }

    private View.OnClickListener recogButtonCliclListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "PRESS BUTTON");
        }
    };


    private void initializeMultiObjectDetector() {
        multiObjectDetector = MultiObjectDetector.getInstance();
        try {
            multiObjectDetector.initialize(this, MODEL_FILE_PATH, LABEL_FILE_PATH);
        } catch (FailedInitializeDetectorException e) {
            e.printStackTrace();
        }
    }

    private void initializeCamera() {
        HandlerThread handlerThread = new HandlerThread("cameraHandler");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        cameraController = CameraController.getInstance();
        try {
            cameraController.initializeCameraDevice(this, IMAGE_WIDTH, IMAGE_HEIGHT, handler, onImageListener);
        } catch (NoCameraFoundException e) {
            Log.e(TAG, "Failed open camera device, please check camera connection.", e);
            exit(-1);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed opne camera device, please check camera connenction.", e);
            exit(-1);
        }
    }

    private void initializeFIRSensor() {
        //Initialzie FIR sensro.
        PeripheralManager mPeripheralManager = PeripheralManager.getInstance();
        gridEyeDriver = GridEyeDriver.getInstance();
        gridEyeDriver.setmPeripheralManager(mPeripheralManager);
        try {
            gridEyeDriver.open(I2C_PORT, ADDRESS_GRID_EYE);
        } catch (IOException e) {
            Log.e(TAG,"Failed open GridEye sensor.");
            exit(-1);
        }
    }

    private ImageReader.OnImageAvailableListener onImageListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            showBitmap = preprocessor.convertImage2Bitmap(image);
            image.close();

            Bitmap recogBitmap = Bitmap.createScaledBitmap(showBitmap,
                    RECOGNITION_IMAGE_WIDTH, RECOGNITION_IMAGE_HEIGHT, false);

            recognitions = new ArrayList<>();

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

            detectionView.setDetectionImage(showBitmap);
            CameraAsyncTask cameraAsyncTask = new CameraAsyncTask(detectionView);
            cameraAsyncTask.execute(showBitmap);

            GridEyeAsyncTask gridEyeAsyncTask = new GridEyeAsyncTask(gridEyeDriver, detectionView);
            gridEyeAsyncTask.setGridEyeListener(gridEyeListener);
            gridEyeAsyncTask.execute();


        }
    };

    private Runnable cameraThread = new Runnable() {
        @Override
        public void run() {
            try {
                cameraController.takePicture();
            } catch (FailedCaptureImageException e) {
                Log.w(TAG, "Failed take picture.", e);
            }
            //if(!doDetection) {
            cameraSensingHandler.postDelayed(this, 600);
            //}else{
            //  cameraSensingHandler.postDelayed(this, 600);
            //}
        }
    };

    private Runnable gridEyeThread = new Runnable() {

        @Override
        public void run() {
            GridEyeAsyncTask gridEyeAsyncTask = new GridEyeAsyncTask(gridEyeDriver, detectionView);
            gridEyeAsyncTask.setGridEyeListener(gridEyeListener);
            gridEyeAsyncTask.execute();
            gridEyeHandler.postDelayed(this, 10);
        }

    };

    private Runnable detectionThread = new Runnable() {
        @Override
        public void run() {

        }
    };

    private GridEyeAsyncTask.GridEyeListener gridEyeListener = new GridEyeAsyncTask.GridEyeListener() {

        @Override
        public void getResult(float[] temperatures) {
            float std = calcStdev(temperatures);
            if (std >= 0.5) {
                List<ObjectInfo> results = searchObjectTemperature(temperatures);
                doDetection = true;
            } else {
                doDetection = false;
            }

        }

    };

    private float calcStdev(float[] data) {
        float sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        float average = sum / (float) data.length;
        sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += (data[i] - average) * (data[i] - average);
        }
        float stdev = sum / data.length;
        return stdev;
    }


    private List<ObjectInfo> searchObjectTemperature(float[] temperatures) {
        List<ObjectInfo> objectsInfo = new ArrayList<>();
        if (recognitions == null) {
            return objectsInfo;
        }
        for (Recognition recognition : this.recognitions) {
            if (recognition.getConfidence() > 0.45) {
                float[] objectTemp = getObjectTemperatures(recognition.getLocation(), temperatures);
                if (objectTemp.length <= 0) {
                    continue;
                }

                float sum = 0;
                for (int i = 0; i < objectTemp.length; i++) {
                    sum += objectTemp[i];
                }
                float average = sum / objectTemp.length;
                ObjectInfo objectInfo = new ObjectInfo(recognition.getTitle(), average);
                objectsInfo.add(objectInfo);
                if (recognition.getTitle().equals("bottle")) {
                    Log.i(TAG, "bottle temprature is " + average);
                }
            }
        }
        return objectsInfo;
    }

    private float[] getObjectTemperatures(RectF location, float[] temperatures) {

        int left = (int) (location.left * 2.13f);
        int top = (int) (location.top * 2.13f);
        int right = (int) (location.right * 2.13f);
        int bottom = (int) (location.bottom * 2.13f);
        int startX = (left / 80) - 1 > 0 ? (left / 80) - 1 : 0;
        int stopX = (right / 80) - 1 < 8 ? (right / 80) - 1 : 7;
        int startY = (top / 60) - 1 > 0 ? (top / 60) - 1 : 0;
        int stopY = (bottom / 60) - 1 < 8 ? (bottom / 60) - 1 : 7;

        int length = ((stopX - startX) + 1) * ((stopY - startY) + 1);
        float[] values = new float[length];

        int i = 0;
        for (int y = startY; y <= stopY; y++) {
            for (int index = startX + (y * 8); index <= stopX + (y * 8); index++) {
                values[i++] = temperatures[index];
            }
        }

        Arrays.sort(values);
        return values;
    }


}
