
package com.qihoo.channel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * 对要改变apk 渠道 channel 信息进行读写改变
 * @author anxianjie-g
 * 2017-4-1
 */
public class Writer implements Const {

    private static final String TAG = "Writer";

    public static boolean saveChannel(String apkFileName, String channel) {
        boolean isSuccess = false;

        if (apkFileName != null && apkFileName.length() > 0) {
            isSuccess = saveChannel(new File(apkFileName), channel);
        }

        Log.i(TAG, "isSuccess=" + isSuccess);
        return isSuccess;
    }

    public static boolean saveChannel(File apkFile, String channel) {
        boolean isSuccess = false;

        if (apkFile != null && apkFile.exists() && apkFile.isFile()) {
            byte[] writeByes = getWriteBytes(channel);
            if (writeByes != null) {
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(apkFile, "rws");
                    long length = raf.length();
                    if (length > MAX_SEARCH_BYTE_LEN) {
                		byte[] buffer = new byte[(int)Math.min(length, 1024)];
                		raf.seek(length - buffer.length);
                		int buffLen = raf.read(buffer, 0, buffer.length);

                        raf.seek(length - getBackByteLength(buffer, buffLen));
                        raf.write(getWriteBytesLength(writeByes.length));
                        raf.write(writeByes);
                        isSuccess = true;
                    } else {
                        Log.i(TAG, "IMPOSSIBLE APK SIZE!!!");
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            Log.i(TAG, "PATH ERROR, OR FILE NOT EXIST, OR NOT A FILE");
        }

        Log.d(TAG, "isSuccess=" + isSuccess);
        return isSuccess;
    }

    private static byte[] getWriteBytes(String channel) {
        byte[] writeBytes = null;
        Log.d(TAG, "channel=" + channel);
        if (channel != null) {
            int channelLen = channel.length();
            Log.d(TAG, "channel.length()=" + channelLen);
            if (channelLen > 0 && channelLen <= MAX_CHANNEL_STRING_LEN) {
                byte[] channelBytes = EncryptUtil.fromPlainToDesByteArray(channel, D_KEY);
                if (channelBytes != null) {
                    Log.printBytes(TAG, channelBytes);
                    int len = channelBytes.length;
                    if (len > 0 && len <= MAX_CHANNEL_BYTE_LEN) {

                        byte[] md5Bytes = null;
                        try {
                            String md5 = Md5Util.md5LowerCase(channel);
                            md5Bytes = md5.getBytes(DEFAULT_CHARSET);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        int i = 0;

                        if (md5Bytes != null) {
                            writeBytes = new byte[MD5_LEN + MD5_MAGIC_LEN + BEG_MAGIC_LEN + len + END_MAGIC_LEN];

                            for (int k = 0; k < MD5_LEN; k++, i++) {
                                writeBytes[i] = (byte) (md5Bytes[k] - MD5_OFFSET);
                            }

                            for (int k = 0; k < MD5_MAGIC_LEN; k++, i++) {
                                writeBytes[i] = MD5_MAGIC[k];
                            }
                        } else {
                            writeBytes = new byte[BEG_MAGIC_LEN + len + END_MAGIC_LEN];
                        }

                        for (int k = 0; k < BEG_MAGIC_LEN; k++, i++) {
                            writeBytes[i] = BEG_MAGIC[k];
                        }

                        for (int k = 0; k < len; k++, i++) {
                            writeBytes[i] = channelBytes[k];
                        }

                        for (int k = 0; k < END_MAGIC_LEN; k++, i++) {
                            writeBytes[i] = END_MAGIC[k];
                        }

                    } else {
                        Log.d(TAG, "CHANNEL BYTES OVER SIZE!!!");
                    }
                } else {
                    Log.d(TAG, "ENCRYPT EXCEPTION!!!");
                }
            } else {
                Log.d(TAG, "CHANNEL STRING OVER SIZE!!!");
            }
        }

        return writeBytes;
    }

    /**
     * Android 5.0开始，需要在末尾修改粘贴数据的长度
     * @param length
     * @return
     */
    private static byte[] getWriteBytesLength(int length) {
        byte[] lengthBytes = new byte[2];
        lengthBytes[0] = (byte)(length & 0x00FF);
        lengthBytes[1] = (byte)((length & 0xFF00) >> 8);
        Log.i(TAG, "getWriteBytesLength length= " + length + " = " + Integer.toHexString(lengthBytes[0]) + Integer.toHexString(lengthBytes[1]));
        return lengthBytes;
    }


    /**
     * 
     * @param buffer
     * @param buffLen
     * @return
     */
    private static int getBackByteLength(byte[] buffer, int buffLen) {
		if (buffLen > 0) {
			byte[] magicDirEnd = { 0x50, 0x4b, 0x05, 0x06 };

			for (int i = buffLen - magicDirEnd.length - 18; i >= 0; i--) {
				boolean isMagicStart = true;
				for (int k = 0; k < magicDirEnd.length; k++) {
					if (buffer[i + k] != magicDirEnd[k]) {
						isMagicStart = false;
						break;
					}
				}
				if (isMagicStart) {
					return (buffLen - (i + 20));
				}
			}
		}

		return 2;
	}

}
