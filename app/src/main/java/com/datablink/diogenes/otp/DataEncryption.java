package com.datablink.diogenes.otp;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.x500.X500Principal;


/**
 * Created by diogenes on 02/08/17.
 * Encrypts/ Decrypts Sensitive data.
 */

public class DataEncryption {

    private KeyStore mKeystore;
    private Context mContext;
    private String mAlias;
    private List<String> keyAliases;

    private  SecretKey mSecretKey;
    private  KeyPair mKeyPair;

    private byte[] mIv;

    private static final String TRANSFORMATION_SYMMETRIC = "AES/GCM/NoPadding";
    private static final String TRANSFORMATION_ASYMMETRIC = "RSA/ECB/PKCS1Padding";


    public DataEncryption(Context context, String alias) {

        mContext = context;
        mAlias = alias;

        // Get instance of keystore
        try {
            mKeystore = KeyStore.getInstance("AndroidKeyStore");
            mKeystore.load(null);
        }
        catch(Exception e) {}

        retrieveKeys();

    }

    public void createNewKeys() {


        try {

            // Don't create key if exists
            if(mKeystore.containsAlias(mAlias)) {
                mSecretKey = (SecretKey) mKeystore.getKey(mAlias,null);
                return;
            }



            // Generate Symmetric keys
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                // Get keyGenerator instance
                final KeyGenerator keyGenerator = KeyGenerator
                        .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

                // Setup properties for the key
                final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(mAlias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build();

                keyGenerator.init(keyGenParameterSpec);

                mSecretKey = keyGenerator.generateKey();


            } else

                // Generate Asymmetric key for older android versions
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    end.add(Calendar.YEAR, 1);
                    KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                            .setAlias(mAlias)
                            .setSubject(new X500Principal("CN=OTP, O=Datablink"))
                            .setSerialNumber(BigInteger.ONE)
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .build();

                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                    generator.initialize(spec);

                    mKeyPair = generator.generateKeyPair();


                } else {

                    // TODO Lower than 4.3
                }

        }catch (NoSuchProviderException| NoSuchAlgorithmException|
                InvalidAlgorithmParameterException e ){
            e.printStackTrace();

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    public void deleteKey(){

        try {
            mKeystore.deleteEntry(mAlias);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    // Retrieve available keys for current alias
    private void retrieveKeys(){

        keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = mKeystore.aliases();

            while (aliases.hasMoreElements()){
                keyAliases.add(aliases.nextElement());
            }

        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

    }


    public byte[] encryptString(String text){

        try {
            // For newer android versions
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                // Create cipher and initialize it with secretkey created, encryption mode and type
                final Cipher cipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC);
                cipher.init(Cipher.ENCRYPT_MODE, mSecretKey);

                // store IV for future use
                mIv = cipher.getIV();

                // return encrypted data
                return (cipher.doFinal(text.getBytes("UTF-8")));


            } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                // Get public and private keys
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) mKeystore.getEntry(mAlias, null);
                RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

                // Get cipher and initialize it
                Cipher input = Cipher.getInstance(TRANSFORMATION_ASYMMETRIC, "AndroidOpenSSL");
                input.init(Cipher.ENCRYPT_MODE, publicKey);

                // Encrypt data
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CipherOutputStream cipherOutputStream = new CipherOutputStream(
                        outputStream, input);
                cipherOutputStream.write(text.getBytes("UTF-8"));
                cipherOutputStream.close();

                return outputStream.toByteArray();


            } else {
                return null;
            }
        }catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException |
                IOException | BadPaddingException | IllegalBlockSizeException | UnrecoverableEntryException | KeyStoreException| NoSuchProviderException e){
            e.printStackTrace();
        }

        return null;
    }


    // Lower than M and higher than KITKAT
    public String decryptString(byte[] cipherText){

        try {
            // Get public and private key
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) mKeystore.getEntry(mAlias, null);
            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

            Cipher output = Cipher.getInstance(TRANSFORMATION_ASYMMETRIC, "AndroidOpenSSL");
            output.init(Cipher.DECRYPT_MODE, privateKey);


            // Generate decrypted data using keys
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(cipherText), output);

            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            return new String(bytes, 0, bytes.length, "UTF-8");

        }catch (NoSuchPaddingException |
                NoSuchAlgorithmException| NoSuchProviderException| InvalidKeyException|
                IOException| UnrecoverableEntryException| KeyStoreException e){
            e.printStackTrace();

        }

        return "";
    }

    // Higher than Marshmallow
    public String decryptString(byte[] cipherText, byte[] iv) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return "";

        try {
            // Get secretKey from keystore
            final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) mKeystore
                    .getEntry(mAlias, null);
            final SecretKey secretKey = secretKeyEntry.getSecretKey();

            // Get cipher and specify Authentication tag length
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC);
            final GCMParameterSpec spec;
            spec = new GCMParameterSpec(128, iv);

            // Initialize decryption process
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return new String(cipher.doFinal(cipherText), "UTF-8");
        }catch (UnrecoverableEntryException|
                NoSuchAlgorithmException|KeyStoreException| NoSuchPaddingException |
                InvalidAlgorithmParameterException| InvalidKeyException| BadPaddingException|
                IllegalBlockSizeException| UnsupportedEncodingException e ){

            e.printStackTrace();
        }

        return "";
    }


    public byte[] getmIv() {
        return mIv;
    }
}
