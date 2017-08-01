package com.datablink.diogenes.otp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getKeyButton = (Button) findViewById(R.id.getKeyButton);
        deleteDataButton = (Button) findViewById(R.id.deleteDataButton);
        otpNumberText = (TextView) findViewById(R.id.otpNumberText);
        tokenLabelText = (TextView) findViewById(R.id.labelText);


        // Try to load stored data
        QRCodeData storedData = loadData();

        if(storedData != null){

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
                saveData(mData);

                updateLabels();

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

            getKeyButton.setVisibility(View.VISIBLE);

        }else{

            // Error
            Log.d("Error", "deleteData");


        }

    }



    //------------------------------------- Helpers ----------------------------------------------//

    public Boolean deleteData(){

        return true;

    }

    public QRCodeData loadData(){

        return null;

    }

    public Boolean saveData(QRCodeData toSave){

        return true;

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

        // Generate OTP and show it
        int otpNumber = getOTP(mData.getKey());
        otpNumberText.setText(Integer.toString(otpNumber));

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

    private String key;
    private String label;

    public QRCodeData(String content){

        // Process String and return QRCodeData object

        try {

            JSONObject json = new JSONObject(content);

            this.key = json.getString("key");
            this.label = json.getString("label");

            Log.d("key", key);
            Log.d("label", label);


        } catch (JSONException e) {

            e.printStackTrace();

        }

    }

    public String getKey() {
        return key;
    }


    public String getLabel() {
        return label;
    }


}
