package ka.masato.aithermalcam;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import ka.masato.library.ai.ssddetection.model.Recognition;

import java.util.ArrayList;

public class AsyncTastSample extends AsyncTask<Bitmap, Void, ArrayList<Recognition>> {


    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected ArrayList<Recognition> doInBackground(Bitmap... bitmaps) {

        ArrayList<Recognition> result = new ArrayList<>();
        return result;

    }


    @Override
    protected void onPostExecute(ArrayList<Recognition> result) {

    }

}
