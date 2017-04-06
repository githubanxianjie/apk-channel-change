
package com.qihoo.channel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * 
 * 读取apk尾部信息
 * @author anxianjie-g
 * 2017-4-1
 */
public class Reader implements Const {

    private static final String TAG = "Reader";


    private static final String ERR_CHANNEL_MD5 = ERROR + "01";

    private static final String ERR_FILE_NOT_FOUND1 = ERROR + "02";

    private static final String ERR_FILE_NOT_FOUND2 = ERROR + "03";

    private static final String ERR_IO_EXCEPTION = ERROR + "04";

    private static final String ERR_CHANNEL_LENGTH1 = ERROR + "05";
    
    private static final String ERR_CHANNEL_LENGTH2 = ERROR + "06";
    
    private static final String ERR_CHANNEL_CHAR_ILLEGAL = ERROR + "07";

    private static final String ERR_BEG_MAGIC_NOT_FOUND = ERROR + "08";
    
    private static final String ERR_PARSE_CHANNEL_NULL = ERROR + "09";
    
    private static final String ERR_DECRYPT_RESULT_NULL = ERROR + "10";
    

    // 尾部标记没找到，这种情况有可能是没有在apk尾部写入渠道，也可能是被破坏。
    private static final String INFO_END_MAGIC_NOT_FOUND = DEFAULT;
    
    private static final String INFO_CHANNEL_NOT_LOAD = DEFAULT + "1";

    public static String loadChannel(String fullPathApkName) {
        return loadChannel(getApkFile(fullPathApkName));
    }

    public static String loadChannel(File apkFile) {
        String channel = PREFIX + INFO_CHANNEL_NOT_LOAD;
        if (apkFile != null) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(apkFile, "r");
                long length = raf.length();
                if (length > MAX_SEARCH_BYTE_LEN) {
                    byte[] bytes = new byte[MAX_SEARCH_BYTE_LEN];
                    raf.seek(length - MAX_SEARCH_BYTE_LEN);
                    raf.readFully(bytes);
                    Log.d(TAG, bytes.toString());
                    String ch = parseChannelFromEnd(bytes);
                    if (ch != null && ch.length() > 0) {
                        if (ch.startsWith(ERROR)) {
                            channel = ch;
                        } else {
                            channel =ch;
//                            channel = PREFIX + ch;
                            if (isChannelLengthLegal(ch)) {
                                if (!isChannelCharLegal(ch)) {
                                    Log.i(TAG, "Channel包含不支持字符");
                                    channel = ERR_CHANNEL_CHAR_ILLEGAL;
                                } 
                            } else {
                                Log.i(TAG, "Channel长度不合法");
                                channel = ERR_CHANNEL_LENGTH2;
                            }
                        } 
                    } else {
                        channel = ERR_PARSE_CHANNEL_NULL;
                    }
                }
            } catch (FileNotFoundException e) {
                Log.i(TAG, "读取APK文件异常，文件找不到");
                channel = ERR_FILE_NOT_FOUND1;
                e.printStackTrace();
            } catch (IOException e) {
                channel = ERR_IO_EXCEPTION;
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
        } else {
            Log.i(TAG, "指定目录下APK文件找不到（不支持系统APK目录）");
            channel = ERR_FILE_NOT_FOUND2;
        }

        Log.d(TAG, "channel=" + channel);
        return channel;
    }

    private static String parseChannelFromEnd(byte[] data) {
        String channel = null;
        if (data != null) {
            ChannelBytes cbs = parseChannelBytesFromEnd(data);
            if (cbs != null) {
                int begIndex = cbs.mBegMagicStartIdx;
                if (cbs.mBegMagicStartIdx >= 0 && cbs.mBytes != null) {
                    channel = EncryptUtil.fromDesByteArrayToString(cbs.mBytes, D_KEY);
                    if (channel != null && channel.length() > 0) {
                        if (begIndex >= (MD5_MAGIC_LEN + MD5_LEN)) {
                            if (isMatchMagic(data, begIndex - MD5_MAGIC_LEN, MD5_MAGIC, MD5_MAGIC_LEN)) {
                                byte[] md5Bytes = new byte[MD5_LEN];
                                for (int i = 0, j = begIndex - MD5_MAGIC_LEN - MD5_LEN; i < MD5_LEN; i++, j++) {
                                    md5Bytes[i] = (byte) (data[j] + MD5_OFFSET);
                                }
                                
                                String readMd5 = new String(md5Bytes);
                                String md5 = Md5Util.md5LowerCase(channel);
                                if (md5 != null && !md5.equals(readMd5)) {
                                    // 被篡改
                                    channel = ERR_CHANNEL_MD5;
                                    Log.i(TAG, "校验失败，数据被篡改");
                                }
                            } else {
                                // 没有找到MD5_MAGIC，不验证MD5
                                Log.i(TAG, "没有找到MM，跳过校验");
                            }
                        } else {
                            // MD5长度不够，不验证MD5
                            Log.i(TAG, "校验数据长度不够，跳过校验");
                        }
                    } else {
                        channel = ERR_DECRYPT_RESULT_NULL;
                        Log.i(TAG, "解密异常，数据被篡改");
                    }
                } else {
                    // mBegMagicStartIdx<0 都是parseChannelBytesFromEnd中的异常状态
                    if (cbs.mBytes != null) {
                        try {
                            channel = new String(cbs.mBytes, DEFAULT_CHARSET);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            Log.d(TAG, "cbs.mBytes编码异常");
                        }
                    }
                }
            } else {
                // 不应该到这里。
                Log.d(TAG, "cbs=null，不应该到这里");
            }
        }

        return channel;
    }

    // 从后往前
    private static ChannelBytes parseChannelBytesFromEnd(byte[] data) {
        ChannelBytes cb = new ChannelBytes();
        if (data != null) {
            int maxBegMagicStartIdx = data.length - BEG_MAGIC_LEN - END_MAGIC_LEN;
            int maxEndMagicStartIdx = data.length - END_MAGIC_LEN;

            if (maxBegMagicStartIdx >= 0 && maxEndMagicStartIdx > BEG_MAGIC_LEN) {
                int begMagicStartIdx = -1;
                int endMagicStartIdx = -1;

                for (int i = maxEndMagicStartIdx; i >= BEG_MAGIC_LEN; i--) {
                    if (isMatchMagic(data, i, END_MAGIC, END_MAGIC_LEN)) {
                        endMagicStartIdx = i;
                        Log.d(TAG, "endMagicStartIdx=" + endMagicStartIdx);
                        break;
                    }
                }

                if (endMagicStartIdx > 0) {
                    for (int j = endMagicStartIdx - BEG_MAGIC_LEN; j >= 0; j--) {
                        if (isMatchMagic(data, j, BEG_MAGIC, BEG_MAGIC_LEN)) {
                            begMagicStartIdx = j;
                            Log.d(TAG, "begMagicStartIdx=" + begMagicStartIdx);
                            break;
                        }
                    }

                    if (begMagicStartIdx >= 0) {
                        int chBytesLen = endMagicStartIdx - begMagicStartIdx - BEG_MAGIC_LEN;
                        if (chBytesLen > 0) {
                            Log.d(TAG, "chBytesLen=" + chBytesLen);
                            byte[] bytes = new byte[chBytesLen];
                            for (int k = 0, l = begMagicStartIdx + BEG_MAGIC_LEN; k < chBytesLen; k++, l++) {
                                bytes[k] = data[l];
                            }
                            cb.mBytes = bytes;
                            cb.mBegMagicStartIdx = begMagicStartIdx;
                        } else {
                            // channel长度错误
                            try {
                                Log.i(TAG, "channel长度错误");
                                cb.mBegMagicStartIdx = -1;
                                cb.mBytes = ERR_CHANNEL_LENGTH1.getBytes(DEFAULT_CHARSET);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // 找不到BEG_MAGIC
                        try {
                            Log.i(TAG, "找不到BM，数据被篡改");
                            cb.mBegMagicStartIdx = -1;
                            cb.mBytes = ERR_BEG_MAGIC_NOT_FOUND.getBytes(DEFAULT_CHARSET);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // 找不到END_MAGIC
                    try {
                        Log.i(TAG, "找不到EM，没有读到渠道，使用默认");
                        cb.mBegMagicStartIdx = -1;
                        cb.mBytes = INFO_END_MAGIC_NOT_FOUND.getBytes(DEFAULT_CHARSET);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        return cb;

    }

    private static class ChannelBytes {

        public ChannelBytes() {
            mBytes = null;
            mBegMagicStartIdx = -1;
        }

        public byte[] mBytes;
        public int mBegMagicStartIdx;
    }

    private static boolean isMatchMagic(byte[] data, int startIdx, byte[] magic, int magicSize) {
        if (data != null && startIdx >= 0) {
            int len = data.length;
            if (len > 0) {
                int endIdx = startIdx + magicSize - 1;
                if (len > endIdx) {
                    for (int i = 0, j = startIdx; i < magicSize; i++, j++) {
                        if (data[j] != magic[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private static File getApkFile(String fullPathApkName) {
        File apk = null;
        if (fullPathApkName != null && fullPathApkName.length() > 0) {
            apk = new File(fullPathApkName);
        }
        return apk;
    }
    
    public static boolean isChannelLengthLegal(String channel) {
        if (channel != null) {
            int chLen = channel.length();
            if (chLen <= 0 || chLen > Const.MAX_CHANNEL_STRING_LEN) {
                return false;
            }
            
            return true;
        }

        return false;
    }
    
    public static boolean isChannelCharLegal(String channel) {
        if (channel != null) {
            int chLen = channel.length();
            for (int j = 0; j < chLen; j++) {
                char val = channel.charAt(j);
                if (!isCharLegal(val)) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    private static boolean isCharLegal(char val) {
        if (val >= '0' && val <= '9') {
            return true;
        }

        if (val >= 'a' && val <= 'z') {
            return true;
        }

        if (val >= 'A' && val <= 'Z') {
            return true;
        }

        if (val == '_') {
            return true;
        }

        return false;
    }      

}
