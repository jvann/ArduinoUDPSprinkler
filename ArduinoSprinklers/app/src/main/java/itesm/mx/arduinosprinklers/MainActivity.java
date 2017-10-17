package itesm.mx.arduinosprinklers;

import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Switch;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.CompoundButton;
import android.support.v7.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Public Statics
    public static TextView tvSoilHumidity;
    public static TextView tvLight;
    public static TextView tvTemperature;
    public static TextView tvHumidity;
    public static TextView tvLastSprinkler;
    public static Button updateBtn;
    public static ProgressBar pbar;

    //EditTexts
    EditText etSoilMin;
    EditText etSoilMax;
    EditText etLightMin;
    EditText etLightMax;

    //Buttons
    Button setVals;

    //Switch
    Switch sbPower;

    //IP address. For connection with arduino MCU.
//    final byte [] IP={10,15,(byte)239,93};//SelfMCU
    final byte [] IP={10,15,(byte)239,98};//SelfMCU
//    final byte [] IP={10, 12, 70, (byte)183};
    final int portNum = 4590;//Port number for connection with arduino
//    final int portNum = 8888;//Port number for connection with arduino

    fetchData process;//Connection.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //POWER: "1 (0:1)\0"
        //SOILMIN: "2 (0:1023)\0"
        //SOILMAX: "3 (0:1023)\0"
        //LIGHTMIN: "4 (0:1023)\0"
        //LIGHTMAX: "5 (0:1023)\0"
        // "\0" character clears and end the buffer in Arduino inputstream.

        tvSoilHumidity = (TextView) findViewById(R.id.text_valueSoilHumidty);
        tvLight = (TextView) findViewById(R.id.text_valueLight);
        tvTemperature = (TextView) findViewById(R.id.text_valueTemperature);
        tvHumidity = (TextView) findViewById(R.id.text_valueHumidity);
        tvLastSprinkler = (TextView) findViewById(R.id.text_valueYesterday);
        etSoilMin = (EditText) findViewById(R.id.edit_soilMIN);
        etSoilMax = (EditText) findViewById(R.id.edit_soilMAX);
        etLightMin = (EditText) findViewById(R.id.edit_lightMIN);
        etLightMax = (EditText) findViewById(R.id.edit_lightMAX);
        updateBtn = (Button) findViewById(R.id.button_update);
        setVals = (Button) findViewById(R.id.button_setRange);
        pbar = (ProgressBar) findViewById(R.id.update_bar);
        sbPower = (Switch) findViewById(R.id.switch_controller);

        connectionThingSpeak();//Creates connection with thingspeak and return JSON.

        //Button Controllers.
        updateBtn.setOnClickListener(this);
        sbPower.setOnClickListener(this);
        setVals.setOnClickListener(this);

        sbPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                if (b) {
                    sendUDPPacket("1 1");//Power ON
                } else {
                    sendUDPPacket("1 0");//Power OFF
                }
            }
        });
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.button_update:
                updateBtn.setVisibility(View.INVISIBLE);
                pbar.setVisibility(View.VISIBLE);
                connectionThingSpeak();
                break;
            case R.id.button_setRange:

                String s = "";
                String e = "UDP Error:(";

                if (!etSoilMin.getText().toString().isEmpty() && (Integer.parseInt(etSoilMin.getText().toString()) <= 1023) && (Integer.parseInt(etSoilMin.getText().toString()) >= 0) ){
                    s += "2 " + etSoilMin.getText().toString() + " ";
                } else if (!etSoilMin.getText().toString().isEmpty()) {
                    e += " Soil Min,";
                }
                if (!etSoilMax.getText().toString().isEmpty() && (Integer.parseInt(etSoilMax.getText().toString()) <= 1023) && (Integer.parseInt(etSoilMax.getText().toString()) >= 0) ){
                    s += "3 " + etSoilMax.getText().toString() + " ";
                } else if (!etSoilMax.getText().toString().isEmpty()) {
                    e += " Soil Max,";
                }
                if (!etLightMin.getText().toString().isEmpty() && (Integer.parseInt(etLightMin.getText().toString()) <= 1023) && (Integer.parseInt(etLightMin.getText().toString()) >= 0) ){
                    s += "4 " + etLightMin.getText().toString() + " ";
                } else if (!etLightMin.getText().toString().isEmpty()) {
                    e += " Light Min,";
                }
                if (!etLightMax.getText().toString().isEmpty() && (Integer.parseInt(etLightMax.getText().toString()) <= 1023) && (Integer.parseInt(etLightMax.getText().toString()) >= 0) ){
                    s += "5 " + etLightMax.getText().toString() + " ";
                } else if (!etLightMax.getText().toString().isEmpty()) {
                    e += " Light Max,";
                }

                if (!s.isEmpty()) {
                    sendUDPPacket(removeLastChar(s));//Send UDP Packet qith the new values.
                }

                if (!e.equalsIgnoreCase("UDP Error:(")) {
                    e = removeLastChar(e) + ") not sent.";
                    Toast.makeText(this, e, Toast.LENGTH_SHORT).show();//Tells if certain udp was not sent.
                }

                break;

        }
    }

    public void connectionThingSpeak() {
        process = new fetchData();//Costum class to fetch data from Thing Speak
        process.execute();
    }

    public void sendUDPPacket(String m) {

        //Responsible for sendind UDP packets to the arduino.
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        final String msg = m + "\0"; // To make end packet at arduino.

        new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress serverAddress = InetAddress.getByAddress(IP);

                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = null;
                    //DatagramPacket(msg, msg length, address, port)

                    outputStream.write( msg.getBytes() );

                    final byte c[] = outputStream.toByteArray( );

                    packet = new DatagramPacket(c, c.length,
                            serverAddress, portNum);

                    Log.d("Packet", packet.getData().toString());
                    socket.send(packet);
                    socket.close();
                } catch (final UnknownHostException e) {
                    e.printStackTrace();
                } catch (final SocketException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public String removeLastChar(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, s.length()-1);
    }


}
