# ercode
二维码 \n
二维码扫描，二维码生成并保存到本地图片，识别本地图片二维码功能\n
可以引用arr 或者 项目。具体使用参考Demo\n
//aar引用
implementation(name: 'zxinglib-release', ext: 'aar')\n
implementation 'com.google.zxing:core:3.3.2'\n
//项目引用
implementation project(path: ':zxinglib')\n