# apk-channel-change
将一个apk 通过工具类动态改变渠道号（channel）信息

通过 Reader类的 loadChannel（）方法，

根据传入需要改变apk的file  进行查询当前 apk的渠道信息
String loadChannel =Reader.loadChannel(file);

通过 Writer类的 saveChannel（） 方法，传入需要改变的apk file
和需要更改 才channel 的value 信息
 Writer.saveChannel(file, "gu_123456789");
 
 
 然后，在调用Reader.loadChannel(file); 就得到你们更改渠道信息的值
