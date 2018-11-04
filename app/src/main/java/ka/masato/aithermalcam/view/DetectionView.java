package ka.masato.aithermalcam.view;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import ka.masato.library.ai.ssddetection.model.Recognition;

import java.util.ArrayList;

public class DetectionView extends View {

    private static final int OFFSET_X = 80;
    private static final int OFFSET_Y = 80;
    private static final float SCALE = (float) 0.7;

    private Paint paint;
    private float strokeWidth = 2.0f;

    private ArrayList<Recognition> detectionObjects;
    private Bitmap cameraImage;
    private float[] temperatures;
    private ArrayList<SensingData> sensingDataList;

    public DetectionView(Context context) {
        super(context);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);
        canvas.drawColor(Color.argb(255, 255, 255, 255));//set background.
        // ペイントストロークの太さを設定
        paint.setStrokeWidth(strokeWidth);
        // Styleのストロークを設定する
        paint.setStyle(Paint.Style.FILL);

        canvas.scale(SCALE, SCALE);
        if (cameraImage != null) {
            float scaleWidth = (float) getWidth() / cameraImage.getWidth(); //TODO may be prepare set up file into Raspberri pi system.
            float scaleHeight = (float) getHeight() / cameraImage.getHeight();
            RectF dst = new RectF();
            dst.set(OFFSET_X, OFFSET_Y,
                    (float) cameraImage.getWidth() + OFFSET_X,
                    (float) cameraImage.getHeight() + OFFSET_Y);
            canvas.drawBitmap(cameraImage, null, dst, paint);
        }
        drawResultRectangle(canvas);
        drawFRIHeatMap(canvas);
        canvas.scale((float) 1.0, (float) 1.0);
    }

    private void drawFRIHeatMap(Canvas canvas) {
        if (sensingDataList == null) {
            return;
        }
        for (int i = 0; i < sensingDataList.size(); i++) {
            paint.setStyle(Paint.Style.FILL);
            SensingData sensingData = sensingDataList.get(i);
            int color = sensingData.getColorBarRGB();
            paint.setColor(color);
            canvas.drawRect(sensingData.getLeft() + OFFSET_X, sensingData.getTop() + OFFSET_Y,
                    sensingData.getRight() + OFFSET_X, sensingData.getBottom() + OFFSET_Y, paint);
            paint.setColor(Color.argb(255, 0, 0, 0));
            String value = Double.toString(sensingData.getValue());
            canvas.drawText(value, sensingData.getLeft() + OFFSET_X + 10, sensingData.getTop() + OFFSET_Y + 20, paint);
        }
    }

    private void drawResultRectangle(Canvas canvas) {
        if (this.detectionObjects != null) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.GREEN);
            for (int i = 0; i < this.detectionObjects.size(); i++) {
                Recognition mRecognition = this.detectionObjects.get(i);
                if (mRecognition.getConfidence() < 0.5) {
                    continue;
                }
                canvas.drawText(mRecognition.getTitle(),
                        mRecognition.getLocation().left * 2.13f,
                        mRecognition.getLocation().top * 2.13f,
                        paint);
                RectF scaleLocation = new RectF(mRecognition.getLocation().left * 2.13f,
                        mRecognition.getLocation().top * 2.13f,
                        mRecognition.getLocation().right * 2.13f,
                        mRecognition.getLocation().bottom * 2.13f);
                canvas.drawRect(scaleLocation, paint);
            }
        }

    }

    public void setCameraImage(Bitmap cameraImage) {
        this.cameraImage = cameraImage;
    }

    public void setDetectionObjects(ArrayList<Recognition> detectionObjects) {
        this.detectionObjects = detectionObjects;
    }

    public void setTemperatures(float[] temperatures) {
        this.temperatures = temperatures;
    }

    public void setSensingDataList(ArrayList<SensingData> sensingDataList) {
        this.sensingDataList = sensingDataList;
    }
}
