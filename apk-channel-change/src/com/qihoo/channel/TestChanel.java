package com.qihoo.channel;

import java.io.File;

/**
 * test
 * 
 * @author anxianjie-g 2017-4-1
 */
public class TestChanel {
	public static void main(String[] args) {
		String loadChannel;
		File file;
		file = new File(
				"C:\\Users\\anxianjie-g\\Desktop\\149623_360Gameunion-CI_signed-1489718193\\GameUnion-CI-2017-03-17_10-30-55_signed_aligned.apk");
		// loadChannel = Reader.loadChannel(file);
//		 Writer.saveChannel(file, "gu_123456789");//gu_123456789：需要更改的渠道号
		loadChannel = Reader.loadChannel(file);
		System.out.println("loadChannel=" + loadChannel);

	}
}
