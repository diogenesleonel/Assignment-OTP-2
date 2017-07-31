package com.datablink.diogenes.otp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    Button getKeyButton;
    Button deleteDataButton;
    TextView otpNumberText;
    TextView tokenLabelText;

    class QRCodeData {
        String key;
        String label;
    }

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

            // Show data label
            tokenLabelText.setText(storedData.label);

            // Disable getKey Button
            getKeyButton.setVisibility(View.INVISIBLE);

            // Generate a new OTP with stored key and show it.
            int otpNumber = getOTP(storedData.key);
            otpNumberText.setText(Integer.toString(otpNumber));
            updateOtpRealTime();


        }



    }

    // Get QR Code String:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //--------------------------------- Button Listeners------------------------------------------//


    public void getKeyClick(View v){

        Log.d("Clicked", "getKeyClick");

        // Read QRCodeData and save it
        QRCodeData readData = readQRCode();
        saveData(readData);

        // Show label
        tokenLabelText.setText(readData.label);

        // Generate OTP and show it
        int otpNumber = getOTP(readData.key);
        otpNumberText.setText(Integer.toString(otpNumber));
        updateOtpRealTime();


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

        // JNI function call
        int otpNumber = 0;

        return otpNumber;

    }

    public QRCodeData readQRCode(){

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();

        return new QRCodeData();

    }

    public void updateOtpRealTime(){


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
    public native String stringFromJNI();


    // Example of a call to a native method
    //TextView tv = (TextView) findViewById(R.id.sample_text);
    //tv.setText(stringFromJNI());
}
