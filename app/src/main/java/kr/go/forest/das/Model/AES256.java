package kr.go.forest.das.Model;

import android.util.Base64;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES256 {

    public static byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    public static String encode(String str, String key){
        try {
            byte[] text = str.getBytes("UTF-8");
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = null;
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret_key, ivSpec);

            return Base64.encodeToString(cipher.doFinal(text), 0);
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return null;
    }

    public static String decode(String str, String key)	 {
        try {
            byte[] textBytes = Base64.decode(str, 0);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
            return new String(cipher.doFinal(textBytes), "UTF-8");
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return  null;
    }
}
