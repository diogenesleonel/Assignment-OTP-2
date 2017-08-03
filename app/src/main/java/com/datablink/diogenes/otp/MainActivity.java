package com.datablink.diogenes.otp;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity {

    Button getKeyButton;
    Button deleteDataButton;
    TextView otpNumberText;
    TextView tokenLabelText;

    private QRCodeData mData;

    private Handler timerHandler;
    private Runnable otpRunnable;
    private static final int UPDATE_INTERVAL_SECONDS = 30;

    private SharedPreferences sp;
    private static final String SP_KEY = "key";
    private static final String SP_KEY_IV = "key_IV";
    private static final String SP_LABEL = "label";
    private static final String SP_LABEL_IV = "label_IV";

    private DataEncryption mDataEncryption;
    private static final String KEYSTORE_ALIAS = "pros";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("LifeCycle", "OnCreate");


        // References
        sp = getSharedPreferences(getApplicationContext().getPackageName() + ".data",
                Context.MODE_PRIVATE);

        getKeyButton = (Button) findViewById(R.id.getKeyButton);
        deleteDataButton = (Button) findViewById(R.id.deleteDataButton);
        otpNumberText = (TextView) findViewById(R.id.otpNumberText);
        tokenLabelText = (TextView) findViewById(R.id.labelText);


    }

    public void loadStoredData(){
        /* Try to load stored data */
        mData = loadData();

        if(mData != null){

            getKeyButton.setVisibility(View.INVISIBLE); // Disable getKey Button

            updateLabels();

        }
    }

    @Override
    protected void onPause() {

        Log.d("LifeCycle", "Pause");

        if(timerHandler != null)
            timerHandler.removeCallbacksAndMessages(null);// Stop timer

        super.onPause();
    }

    @Override
    protected void onResume() {

        Log.d("LifeCycle", "Resume");

        mDataEncryption = new DataEncryption(this, KEYSTORE_ALIAS);
        mDataEncryption.createNewKeys();

        loadStoredData();

        super.onResume();
    }

    /** Get QR Code String and process it. */
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

        readQRCode(); // Read QRCodeData and save it

    }

    public void deleteDataClick(View v){

        Log.d("Clicked", "deleteDataClick");

        if(deleteData()){

            Log.d("Success", "deleteData"); // Success

            tokenLabelText.setText("");
            otpNumberText.setText("");
            getKeyButton.setVisibility(View.VISIBLE);

        }else{

            Log.d("Error", "deleteData"); // Error

        }

    }

    //------------------------------------- Helpers ----------------------------------------------//

    public Boolean deleteData(){

        SharedPreferences.Editor editor = sp.edit();
        editor.clear();

        mDataEncryption.deleteKey(); // Delete keys to avoid error when restart app
        mDataEncryption.createNewKeys(); // Restart keys

        return editor.commit();

    }

    public QRCodeData loadData(){

        Log.d("D", "Loading data");

        String key, label;
        String spLabel = sp.getString(SP_LABEL,"") ;
        String spLabelIv = sp.getString(SP_LABEL_IV,"");

        String spKey = sp.getString(SP_KEY,"");
        String spKeyIv = sp.getString(SP_KEY_IV,"");

        byte[] keyCipher, keyIv, labelCipher, labelIv;


        if (spLabel.isEmpty() || spKey.isEmpty())
            return  null;

        // Transform text encoded into byte[]
        keyCipher = Base64.decode(spKey, Base64.DEFAULT);
        labelCipher = Base64.decode(spLabel, Base64.DEFAULT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            keyIv = Base64.decode(spKeyIv, Base64.DEFAULT);
            labelIv = Base64.decode(spLabelIv, Base64.DEFAULT);
            key = mDataEncryption.decryptString(keyCipher, keyIv);
            label = mDataEncryption.decryptString(labelCipher, labelIv);
        } else {
            key = mDataEncryption.decryptString(keyCipher);
            label = mDataEncryption.decryptString(labelCipher);
        }


        Log.d("Decryption [label]", label);
        Log.d("Decryption [key]", key);


        QRCodeData loadData = new QRCodeData(key, label);

        if(loadData.isDataValid())
            return loadData;
        else
            return null ;

    }

    public Boolean saveData(QRCodeData toSave){

        Log.d("D", "Saving data");

        SharedPreferences.Editor editor = sp.edit();

        // Encrypts key
        byte[] keyCipher = mDataEncryption.encryptString(toSave.getKey());
        editor.putString(SP_KEY, Base64.encodeToString(keyCipher, Base64.DEFAULT ));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            editor.putString(SP_KEY_IV, Base64.encodeToString(mDataEncryption.getmIv(), Base64.DEFAULT ));

        // Encrypts label
        byte[] labelCipher = mDataEncryption.encryptString(toSave.getLabel());
        editor.putString(SP_LABEL, Base64.encodeToString(labelCipher, Base64.DEFAULT ));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            editor.putString(SP_LABEL_IV, Base64.encodeToString(mDataEncryption.getmIv(), Base64.DEFAULT ));

        return editor.commit();

    }



    /** Setup ZXing and initialize camera. */
    public void readQRCode(){

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.setPrompt("Scan QR Code");
        integrator.setCameraId(0);
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();

    }

    /**
     *  Update token label and start a timer to
     *  update otp number label in a fixed time interval
     */
    public void updateLabels(){

        // Show label
        tokenLabelText.setText(mData.getLabel());

        timerHandler =  new Handler();

        // Update OTP number every X seconds
        otpRunnable = new Runnable() {
            @Override
            public void run() {

                // Generate OTP and show it
                int otpNumber = generateOtpDigits(mData.getKey());

                otpNumberText.setText(String.format("%06d", otpNumber));

                timerHandler.postDelayed(this, UPDATE_INTERVAL_SECONDS * 1000);
            }
        };

        otpRunnable.run();


    }



    //--------------------------------------- JNI ------------------------------------------------//

    /** Used to load the 'native-lib' library on application startup. **/
    static {
        System.loadLibrary("native-lib");
    }

    /** Native methods that is implemented by the 'native-lib' */
    public native byte[] generateOtp(String key);
    public native int generateOtpDigits(String key);

    /** Convert byte Array to hexadecimal */
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

}


    //--------------------------------------- QR Code Class---------------------------------------//

/** QR Code class to manage read data. **/
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

        //  Process String and return QRCodeData object
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

    /** Validate data */
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
