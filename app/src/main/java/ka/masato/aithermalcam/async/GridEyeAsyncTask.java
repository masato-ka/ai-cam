package ka.masato.aithermalcam.async;

import android.os.AsyncTask;
import android.util.Log;
import ka.masato.aithermalcam.view.DetectionView;
import ka.masato.aithermalcam.view.SensingData;
import ka.masato.grideyelib.driver.GridEyeDriver;

import java.io.IOException;
import java.util.ArrayList;

public class GridEyeAsyncTask extends AsyncTask<Void, Void, ArrayList<SensingData>> {

    private static final String TAG = "GridEyeAsyncTask";

    private GridEyeDriver gridEyeDriver;
    private DetectionView detectionView;
    private GridEyeListener gridEyeListener;

    public GridEyeAsyncTask(GridEyeDriver gridEyeDriver, DetectionView detectionView) {
        this.gridEyeDriver = gridEyeDriver;
        this.detectionView = detectionView;
    }


    @Override
    protected ArrayList<SensingData> doInBackground(Void... voids) {
        float[] temperatures = null;
        try {
            temperatures = gridEyeDriver.getTemperatures();
            if (gridEyeListener != null) {
                gridEyeListener.getResult(temperatures);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed get temperatures ", e);
        }
        ArrayList<SensingData> sensingDataList = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                SensingData sensingData =
                        new SensingData(7 - j, 7 - i, temperatures[j + (i * 8)]);
                sensingDataList.add(sensingData);
            }
        }
        return sensingDataList;
    }

    @Override
    protected void onPostExecute(ArrayList<SensingData> result) {
        detectionView.setSensingDataList(result);
        detectionView.invalidate();
    }


    public void setGridEyeListener(GridEyeListener gridEyeListener) {
        this.gridEyeListener = gridEyeListener;
    }

    public interface GridEyeListener {
        public void getResult(float[] temperatures);
    }
}
