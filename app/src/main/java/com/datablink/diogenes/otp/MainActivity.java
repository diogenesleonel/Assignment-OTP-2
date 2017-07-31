package com.datablink.diogenes.otp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

        return new QRCodeData();

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
