
package com.qihoo.channel;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 加密工具
 * @author anxianjie-g
 * 2017-4-1
 */
public class EncryptUtil {

    private static final String CHARSET_NAME = "UTF-8";

    // 不能修改
    private static final String DES = "DES";

    // 不能修改
    private static final String CBC_MODEL = "DES/CBC/PKCS5Padding";

    /**
     * DES加密
     * 
     * @param plain 待加密字符串
     * @param key DES密钥
     * @return 返回DES加密后byte[]
     */
    public static byte[] fromPlainToDesByteArray(String plain, String key) {
        byte desBytes[] = null;
        if (plain != null && plain.length() > 0) {
            try {
                DESKeySpec dks = new DESKeySpec(key.getBytes(CHARSET_NAME));

                // 创建一个密匙工厂，然后用它把DESKeySpec转换成
                // 一个SecretKey对象
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
                SecretKey secretKey = keyFactory.generateSecret(dks);

                // using DES in CBC mode
                Cipher cipher = Cipher.getInstance(CBC_MODEL);

                // 初始化Cipher对象
                IvParameterSpec iv = new IvParameterSpec(key.getBytes(CHARSET_NAME));
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

                // 执行加密操作
                desBytes = cipher.doFinal(plain.getBytes(CHARSET_NAME));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return desBytes;
    }

    public static String fromDesByteArrayToString(byte[] desByteArray, String key) {
        String plain = null;
        if (desByteArray != null) {
            try {
                DESKeySpec dks = new DESKeySpec(key.getBytes(CHARSET_NAME));

                // 创建一个密匙工厂，然后用它把DESKeySpec转换成
                // 一个SecretKey对象
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
                SecretKey secretKey = keyFactory.generateSecret(dks);

                // using DES in CBC mode
                Cipher cipher = Cipher.getInstance(CBC_MODEL);

                // 初始化Cipher对象
                IvParameterSpec iv = new IvParameterSpec(key.getBytes(CHARSET_NAME));
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

                // 执行加密操作
                byte[] plainBytes = cipher.doFinal(desByteArray);

                plain = new String(plainBytes, CHARSET_NAME);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return plain;
    }

}
