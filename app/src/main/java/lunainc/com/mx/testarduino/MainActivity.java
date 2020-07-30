package lunainc.com.mx.testarduino;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static final String MESSAGE_STATUS = "message_status";

    @BindView(R.id.buttonStart)
    Button startButton;

    @BindView(R.id.buttonSend)
    Button sendButton;

    @BindView(R.id.buttonClear)
    Button clearButton;

    @BindView(R.id.buttonStop)
    Button stopButton;

    @BindView(R.id.textView)
    TextView textView;
    @BindView(R.id.editText)
    EditText editText;


    public final String ACTION_USB_PERMISSION = "lunainc.mx.com.testarduino.USB_PERMISSION";
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initVars();
    }

    public void initVars() {

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        try {
            setUiEnabled(false);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(broadcastReceiver, filter);
        } catch (Exception ex) {
            Log.e("Error: ", Objects.requireNonNull(ex.getMessage()));
        }
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        clearButton.setEnabled(bool);
        textView.setEnabled(bool);

    }



    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            data = new String(arg0, StandardCharsets.UTF_8);
            tvAppend(textView, data);
        }
    };


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (Objects.equals(intent.getAction(), ACTION_USB_PERMISSION)) {
                    boolean granted = Objects.requireNonNull(intent.getExtras()).getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        connection = usbManager.openDevice(device);
                        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                        if (serialPort != null) {
                            if (serialPort.open()) { //Set Serial Connection Parameters.
                                setUiEnabled(true);
                                serialPort.setBaudRate(115200);
                                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                serialPort.read(mCallback);
                                tvAppend(textView, "Start");

                            } else {
                                Log.d("SERIAL", "PORT NOT OPEN");
                                Toast.makeText(context, "puerto no abierto", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("SERIAL", "PORT IS NULL");
                            Toast.makeText(context, "Null", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                        Toast.makeText(context, "Sin permiso", Toast.LENGTH_SHORT).show();
                    }
                } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    onClickStart(startButton);
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {

                    onClickStop(stopButton);
                    Toast.makeText(context, "Paro el trabajo", Toast.LENGTH_SHORT).show();
                }

        } catch (NullPointerException nullPoin) {
            Log.e("NullPointer", Objects.requireNonNull(nullPoin.getMessage()));
        } catch (Exception e) {
            Log.e("Exception", Objects.requireNonNull(e.getMessage()));
        }
    }


};


    public void onClickStart(View view) {


        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID != 0)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }

    }


    /**
     * Send data to UI
     *
     * @param view vista a la que se dio click
     */
    public void onClickSend(View view) {
        try {
            String string = editText.getText().toString();
            serialPort.write(string.getBytes());
            tvAppend(textView, "\nData Sent : " + string + "\n");
        } catch (NullPointerException nullPoint) {
            Log.e("NullPinter", Objects.requireNonNull(nullPoint.getMessage()));
        } catch (Exception e) {
            Log.e("Exception", Objects.requireNonNull(e.getMessage()));
        }

    }


    /**
     * Stop work with the device
     *
     * @param view vista a la que se dio click
     */
    public void onClickStop(View view) {
        try {
            setUiEnabled(false);
            serialPort.close();
            tvAppend(textView, "Stop");

        } catch (NullPointerException exPointer) {
            Log.e("NullPinter", Objects.requireNonNull(exPointer.getMessage()));
        } catch (Exception e) {
            Log.e("Exception", Objects.requireNonNull(e.getMessage()));
        }
    }


    /**
     * Clear the UI
     *
     * @param view vista a la que se dio click
     */
    public void onClickClear(View view) {
        textView.setText(" ");
    }



    /**
     * Load data into UI
     *
     * @param tv textView donde se colocara el texto
     * @param text valor de contaminación por gas y presion
     */
    @SuppressLint("SetTextI18n")
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(() -> {
            if (!ftext.equals("Start") && !ftext.equals("Stop")) {
                String[] arrOfStr = ftext.toString().split("Gas:");


                for (String a : arrOfStr) {
                    if (a.contains("presion")) {


                        String[] spll = a.split(":");


                        float dat = Float.parseFloat(spll[1]);


                        int valINT = (int) dat;

                        ftv.append("Presion: "+ valINT);


                    } else {
                        ftv.append("Contaminación del gas: " + a.toUpperCase());

                    }
                }


            }

            ftv.append(ftext);
        });
    }


}