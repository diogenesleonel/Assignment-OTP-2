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



public class MainActivity extends AppCompatActivity {

    Button getKeyButton;
    Button deleteDataButton;
    TextView otpNumberText;
    TextView tokenLabelText;

    QRCodeData mData;

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


        Log.d("hmca", byteArrayToHex(hmac));


        return 0;

    }

    public void readQRCode(){

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);
        integrator.setOrientationLocked(false);
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
