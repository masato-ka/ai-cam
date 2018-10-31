package ka.masato.aithermalcam;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.TimingLogger;
import com.google.android.things.pio.PeripheralManager;
import ka.masato.aithermalcam.view.DetectionView;
import ka.masato.aithermalcam.view.SensingData;
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
    private static final int RECOGNITION_IMAGE_WIDTH = 300;
    private static final int RECOGNITION_IMAGE_HEIGHT = 300;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        cameraSensingHandler.post(cameraSensingLoop);

        mainHandler = new Handler();

        detectionView = new DetectionView(this);
        setContentView(detectionView);

        HandlerThread handlerThread = new HandlerThread("grideye");
        handlerThread.start();
        gridEyeHandler = new Handler(handlerThread.getLooper());
        gridEyeHandler.post(sensingGridEye);


    }

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

            Bitmap showBitmap = preprocessor
                    .preprocessImage(image);

            Bitmap recogBitmap = Bitmap.createScaledBitmap(showBitmap,
                    RECOGNITION_IMAGE_WIDTH, RECOGNITION_IMAGE_HEIGHT, false);

            detectionHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        TimingLogger logger = new TimingLogger("LOG_TAG_TEST", "testTimingLogger");
                        ArrayList<Recognition> result = multiObjectDetector.runDetection(recogBitmap, 10);
                        detectionView.setDetectionObjects(result);
                        logger.addSplit("afterDetection");
                        logger.dumpToLog();
                        result.stream().map(e -> e.getTitle()).forEach(System.out::println);
                    } catch (UnInitializeDetectorException e) {
                        Log.d(TAG, "Please initialize detector");
                    }
                }
            });


            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    detectionView.setCameraImage(showBitmap);
                    detectionView.invalidate();
                }
            });

        }
    };


    private Runnable cameraSensingLoop = new Runnable() {
        @Override
        public void run() {
            try {
                cameraController.takePicture();
            } catch (FailedCaptureImageException e) {
                Log.w(TAG, "Failed take picture.", e);
            }
            cameraSensingHandler.postDelayed(this, 30);
        }
    };

    private Runnable sensingGridEye = new Runnable() {
        @Override
        public void run() {
            float[] temperatures = null;
            try {
                temperatures = gridEyeDriver.getTemperatures();
                float stdev = calcStdev(temperatures);
                if (stdev > 1.0) {
                    Log.d(TAG, "STDEV :" + stdev);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed get temperature.", e);
            }
            ArrayList<SensingData> sensingDataList = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    SensingData sensingData = new SensingData(7 - j, 7 - i, temperatures[j + (i * 8)]);
                    sensingDataList.add(sensingData);
                }
            }
            detectionView.setSensingDataList(sensingDataList);

            gridEyeHandler.postDelayed(this, 10);
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

}
