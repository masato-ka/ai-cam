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
import com.google.android.things.pio.PeripheralManager;
import ka.masato.grideyelib.driver.GridEyeDriver;
import ka.masato.library.device.camera.CameraController;
import ka.masato.library.device.camera.ImagePreprocessor;
import ka.masato.library.device.camera.exception.NoCameraFoundException;

import java.io.IOException;

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

    private static final String I2C_PORT = "I2C1";
    private static final int ADDRESS_GRID_EYE = 0x68;

    private GridEyeDriver gridEyeDriver;

    private CameraController cameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeFIRSensor();

        preprocessor = new ImagePreprocessor(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        initializeCamera();


        setContentView(R.layout.activity_main);
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

    private ImagePreprocessor preprocessor;
    private ImageReader.OnImageAvailableListener onImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            Bitmap bitmap = preprocessor
                    .preprocessImage(image);
        }
    };


}
