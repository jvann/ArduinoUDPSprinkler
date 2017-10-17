package itesm.mx.arduinosprinklers;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Jibril on 9/28/17.
 */

public class fetchData extends AsyncTask<Void, Void, Void>{

    String data = "";
    String[] dataArr = new String[5];
    JSONArray JA = null;

    @Override
    protected Void doInBackground(Void... voids) {

        BufferedReader buffer = getBuffer("https://api.thingspeak.com/channels/336999/feeds.json?results=1");
        String line = "";

        while (line != null) {
            try {
                line = buffer.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            data = data + line;
        }

        try {
            JSONObject JO = new JSONObject(data);
            JSONArray JA = JO.getJSONArray("feeds");
            for (int i = 0; i < 4; i++) {
                dataArr[i] = JA.getJSONObject(JA.length()-1).getString("field"+String.valueOf(i+1));

                if (dataArr[i] == "null") {
                    dataArr[i] = "No value";
                }
            }

            dataArr[4] = JA.getJSONObject(JA.length()-1).getString("created_at");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //After the data is read on the background Ui can be modified.

        MainActivity.tvSoilHumidity.setText(this.dataArr[0]);
        MainActivity.tvLight.setText(this.dataArr[1]);
        MainActivity.tvTemperature.setText(this.dataArr[2] + "*C");
        MainActivity.tvHumidity.setText(this.dataArr[3] + "%");
        MainActivity.tvLastSprinkler.setText(this.dataArr[4]);

        //Update button and progressbar.
        MainActivity.pbar.setVisibility(View.INVISIBLE);
        MainActivity.updateBtn.setVisibility(View.VISIBLE);
    }

    protected BufferedReader getBuffer(String sUrl) {

        try {
            URL url = new URL(sUrl);//Brings the last update.
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();//Creates connection
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));//Reads the input.

            return buffer;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
