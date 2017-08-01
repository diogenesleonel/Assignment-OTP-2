package com.datablink.diogenes.otp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    Button getKeyButton;
    Button deleteDataButton;
    TextView otpNumberText;
    TextView tokenLabelText;

    private QRCodeData mData;
    private static final int OFFSET = 10;
    private static final int N_BYTE = 4;

    private Handler timerHandler = new Handler();
    private Runnable otpRunnable;
    private static final int UPDATE_INTERVAL_SECONDS = 30;

    SharedPreferences sp;
    private static final String SP_KEY = "key";
    private static final String SP_LABEL = "label";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(getApplicationContext().getPackageName() + ".data", Context.MODE_PRIVATE);

        getKeyButton = (Button) findViewById(R.id.getKeyButton);
        deleteDataButton = (Button) findViewById(R.id.deleteDataButton);
        otpNumberText = (TextView) findViewById(R.id.otpNumberText);
        tokenLabelText = (TextView) findViewById(R.id.labelText);


        // Try to load stored data
        mData = loadData();

        if(mData != null){

            // Disable getKey Button
            getKeyButton.setVisibility(View.INVISIBLE);

            updateLabels();

        }



    }

    // Get QR Code String:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() != null) {

                Log.d("QR Code Scanned", result.getContents());

                mData = new QRCodeData(result.getContents());

                if(mData.isDataValid()) {

                    getKeyButton.setVisibility(View.INVISIBLE);

                    if(!saveData(mData))
                        Toast.makeText(this, "Could not save data.", Toast.LENGTH_LONG).show();


                    updateLabels();

                }else {

                    Toast.makeText(this, "Could not get a valid information. Please, try again.",
                            Toast.LENGTH_LONG).show();
                }

            } else {

                Log.d("QR Code Scanned", "Cancelled");

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //--------------------------------- Button Listeners------------------------------------------//


    public void getKeyClick(View v){

        Log.d("Clicked", "getKeyClick");

        // Read QRCodeData and save it
        readQRCode();

    }

    public void deleteDataClick(View v){

        Log.d("Clicked", "deleteDataClick");

        if(deleteData()){

            // Success
            Log.d("Success", "deleteData");

            tokenLabelText.setText("");
            otpNumberText.setText("");
            getKeyButton.setVisibility(View.VISIBLE);

        }else{

            // Error
            Log.d("Error", "deleteData");


        }

    }



    //------------------------------------- Helpers ----------------------------------------------//

    public Boolean deleteData(){

        SharedPreferences.Editor editor = sp.edit();
        editor.clear();

        return editor.commit();

    }

    public QRCodeData loadData(){

        QRCodeData loadData = new QRCodeData(sp.getString(SP_KEY,""), sp.getString(SP_LABEL,""));

        if(loadData.isDataValid())
            return loadData;
        else
            return null ;

    }

    public Boolean saveData(QRCodeData toSave){

        SharedPreferences.Editor editor = sp.edit();

        editor.putString(SP_KEY, toSave.getKey());
        editor.putString(SP_LABEL, toSave.getLabel());

        return editor.commit();

    }

    public int getOTP(String key){

        // JNI function call (HMAC- SHA-1 hash)
        byte[] hmac = generateOtp(key);

        // 4 bytes starting at offset 10
        byte[] slice = Arrays.copyOfRange(hmac, OFFSET - 1, OFFSET + N_BYTE - 1);

        // Top bit clear
        slice[0] &= ~(1 << 7);

        // Put bytes into a buffer and convert them to decimal (integer)
        ByteBuffer buffer = ByteBuffer.wrap(slice);
        int decimal = buffer.getInt();

        // Last 6 digits
        int lastDigits = decimal % 1000000;

        Log.d("OTP", "DBC1 (HMAC): " + byteArrayToHex(hmac));
        Log.d("OTP", "DBC2 (4 bytes + top bit clear): " +  byteArrayToHex(slice)  );
        Log.d("OTP", "Decimal Code: " + decimal);
        Log.d("OTP", "TOPT: " + lastDigits);

        return lastDigits;

    }

    public void readQRCode(){

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(1);
        integrator.setOrientationLocked(true);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();

    }


    public void updateLabels(){

        // Show label
        tokenLabelText.setText(mData.getLabel());


        // Update OTP number every X seconds
        otpRunnable = new Runnable() {
            @Override
            public void run() {

                // Generate OTP and show it
                int otpNumber = getOTP(mData.getKey());
                otpNumberText.setText(Integer.toString(otpNumber));

                timerHandler.postDelayed(this, UPDATE_INTERVAL_SECONDS * 1000);
            }
        };

        otpRunnable.run();


    }



    //--------------------------------------- JNI ------------------------------------------------//
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public native byte[] generateOtp(String key);


    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

}


class QRCodeData {

    private String key = "";
    private String label = "";

    private boolean isValidData;

    public QRCodeData(String key, String label) {

        this.key = key;
        this.label = label;

        checkData();

    }

    public QRCodeData(String content){

        // Process String and return QRCodeData object


        try {

            JSONObject json = new JSONObject(content);

            this.key = json.getString("key");
            this.label = json.getString("label");

            Log.d("key", key);
            Log.d("label", label);

            checkData();



        } catch (JSONException e) {

            e.printStackTrace();
            isValidData = false;

        }

    }

    private void checkData(){

        if(key.isEmpty() || label.isEmpty() ||
        key.length() < 32 || key.length() > 32){
            isValidData = false;

        }else{
            isValidData = true;
        }

    }

    public boolean isDataValid(){

        return isValidData;

    }

    public String getKey() {
        return key;
    }


    public String getLabel() {
        return label;
    }


}
