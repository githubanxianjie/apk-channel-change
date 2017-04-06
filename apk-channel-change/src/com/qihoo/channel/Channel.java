
package com.qihoo.channel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Channel {

    private static final String TAG = "Channel";

    private static final String CMD_READ = "read";

    private static final String CMD_WRITE = "write";

    private static final String CMD_WRITE_FORMAT = "写入命令：\n\n" + CMD_WRITE
            + " <fileName|dirName> <channelName1,channelName2...>";

    private static final String CMD_READ_FORMAT = "读取命令：\n\n" + CMD_READ + " <fileName|dirName>";

    private static String sBaseDir;

    private static String[] sChannels;

    private static Boolean sIsCmdRead;

    public static void main(String args[]) {

        Log.i(TAG, "");
        Log.i(TAG, "");

        sBaseDir = null;
        sChannels = null;
        sIsCmdRead = null;

        if (args == null || args.length < 1) {
            Log.i(TAG, "参数个数错误。" + getCmdFormat());
            return;
        }

        String cmd = args[0];
        Log.i(TAG, "参数1：" + args[0]);
        if (cmd == null) {
            Log.i(TAG, "第1个参数(文件名或目录)错误，命令不合法！" + getCmdFormat());
            return;
        }

        if (cmd.equalsIgnoreCase(CMD_READ)) {
            sIsCmdRead = true;
        } else if (cmd.equalsIgnoreCase(CMD_WRITE)) {
            sIsCmdRead = false;
        }
        if (sIsCmdRead == null) {
            Log.i(TAG, "第1个参数(文件名或目录)错误，命令不合法！" + getCmdFormat());
            return;
        }

        if (sIsCmdRead) {
            if (args.length != 2) {
                Log.i(TAG, "参数个数错误。" + getCmdFormat());
                return;
            }
        } else {
            if (args.length != 3) {
                Log.i(TAG, "参数个数错误。" + getCmdFormat());
                return;
            }
        }

        String fileName = args[1];
        Log.i(TAG, "参数2：" + args[1]);
        if (fileName == null || fileName.length() <= 0) {
            Log.i(TAG, "第2个参数(文件名或目录)错误，文件名不合法！" + getCmdFormat());
            return;
        }

        File file = new File(fileName);
        if (file == null || !file.exists()) {
            Log.i(TAG, "第2个参数(文件名或目录)错误，文件名或目录不存在");
            return;
        }

        sBaseDir = file.getParent();
        if (sBaseDir == null) {
            Log.i(TAG, "第2个参数(文件名或目录)错误，父目录不存在");
            return;
        }

        if (!sIsCmdRead) {
            String channelsParam = args[2];
            Log.i(TAG, "参数3：" + args[2]);
            if (channelsParam == null) {
                Log.i(TAG, "第3个参数(渠道)错误，渠道参数未设置。" + getCmdFormat());
                return;
            }

            sChannels = channelsParam.split(",");
            if (sChannels == null) {
                Log.i(TAG, "第3个参数(渠道)错误，渠道参数未设置。" + getCmdFormat());
                return;
            }

            int channelNum = sChannels.length;
            if (channelNum <= 0) {
                Log.i(TAG, "第3个参数(渠道)错误，渠道参数未设置。" + getCmdFormat());
                return;
            }

            for (int i = 0; i < channelNum; i++) {

                String channel = sChannels[i];
                if (Reader.isChannelLengthLegal(channel)) {
                    if (!Reader.isChannelCharLegal(channel)) {
                        Log.i(TAG, "第3个参数中" + channel + "错误，字符不支持，请输入数字字母_-组合");
                        return;
                    }
                } else {
                    Log.i(TAG, "第3个参数中" + channel + "错误，渠道长度必需: >0 and <="
                            + Const.MAX_CHANNEL_STRING_LEN);
                    return;
                }
            }
        }

        if (file.isFile()) {
            handleApkFile(file);
        } else {
            handleDir(file);
        }

    }

    private static String getCmdFormat() {
        final String prefix = "\n\n\n\n命令用法：\n-------------------------\n";
        if (sIsCmdRead != null) {
            if (sIsCmdRead) {
                return prefix + CMD_READ_FORMAT;
            } else {
                return prefix + CMD_WRITE_FORMAT;
            }
        } else {
            return prefix + CMD_WRITE_FORMAT + "; " + CMD_READ_FORMAT;
        }
    }

    private static void handleDir(File dirFile) {
        File childs[] = dirFile.listFiles();
        for (int i = 0; i < childs.length; i++) {
            if (childs[i].isDirectory()) {
                handleDir(childs[i]);
            } else {
                handleApkFile(new File(childs[i].getAbsolutePath()));
            }
        }
    }

    private static void handleApkFile(File file) {
        if (!file.getName().toLowerCase().endsWith(".apk")) {
            Log.i(TAG, "跳过非APK文件：" + file.getAbsolutePath());
            return;
        }

        if (sIsCmdRead) {
            doReadFile(file);
        } else {
            doCopyWriteFile(file);
        }
    }

    private static void doReadFile(File file) {
        Log.i(TAG, "");
        Log.i(TAG, "---------------------------------");
        String channel = Reader.loadChannel(file);
        Log.i(TAG, "读取APK：" + file.getAbsolutePath() + "，渠道号：" + channel);
        Log.i(TAG, "---------------------------------");
        Log.i(TAG, "");
    }

    private static void doCopyWriteFile(File file) {

        if (sChannels == null) {
            Log.i(TAG, "sChannels == null");
            return;
        }

        String folderName = file.getName().trim();
        int pos = folderName.lastIndexOf(".apk");
        folderName = folderName.substring(0, pos).trim();

        String path = file.getParent() + "/write_output/" + folderName + "/";
        File p = new File(path);
        p.mkdirs();

        int len = sChannels.length;
        for (int i = 0; i < len; i++) {
            String ch = sChannels[i];

            String fullName = path + ch + '_' + file.getName();
            Log.i(TAG, "");
            Log.i(TAG, "---------------------------------");
            Log.i(TAG, "处理APK：" + fullName);
            Log.i(TAG, "");
            File target = new File(fullName);
            if (newFile(target)) {
                if (copyFile(file, target)) {
                    boolean isSuccess = Writer.saveChannel(target, ch);
                    Log.i(TAG, "写入APK：" + target + "，渠道号：" + ch + (isSuccess ? "成功！！" : "失败！！"));
                } else {
                    Log.i(TAG, "复制APK：" + file.getName() + "到：" + target + "失败！！");
                }
            } else {
                Log.i(TAG, "创建APK：" + target + "失败！！！");
            }
            Log.i(TAG, "---------------------------------");
            Log.i(TAG, "");

        }

    }

    private static boolean newFile(File file) {
        boolean isSuccess = false;
        try {
            if (file.exists()) {
                Log.i(TAG, "错误APK：" + file.getAbsolutePath() + "文件已存在，无法创建！！");
            } else {
                isSuccess = file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isSuccess;
    }

    // 复制文件
    private static boolean copyFile(File sourceFile, File targetFile) {
        boolean isSuccess = false;
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            // 新建文件输入流并对它进行缓冲
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            // 新建文件输出流并对它进行缓冲
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            // 缓冲数组
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();

            isSuccess = true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            if (inBuff != null) {
                try {
                    inBuff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (outBuff != null) {
                try {
                    outBuff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return isSuccess;
    }

}
