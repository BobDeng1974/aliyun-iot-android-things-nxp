## aliyun-iot-androidthings-nxp
谷歌Android things物联网硬件接入阿里云IoT物联网套件云端实战

### 1.硬件设备

#### android things开发板 NXP Pico i.MX7D
 NXP Pico i.MX7D [完整I/O接口文档](https://developer.android.com/things/hardware/imx7d-pico-io.html)

#### 温湿度传感器 DHT12
DHT12支持I2C，淘宝有售

#### 甲醛传感器 ZE08-CH2O
ZE08支持UART，淘宝有售

### 2.阿里云IoT物联网套件
#### 2.1 开通阿里云IoT物联网套件
[IoT物联网套件官网地址](https://www.aliyun.com/product/iot)
#### 2.2 创建高级版产品
产品属性定义

| 属性名 | 标识符 | 数据类型 | 描述|
| ------| ------ | ------ | ------ |
| 温度 | temperature | float | DHT12传感器采集 |
| 湿度 | humidity | float | DHT12传感器采集 |
| 甲醛浓度 | ch2o | double | ZE08传感器采集 |


![](https://raw.githubusercontent.com/iot-blog/aliyun-iot-android-things-nxp/master/images/iot-product-property.png)

#### 2.3 设备端开发

a) 使用Android Studio创建Android things工程，添加网络权限
```
<uses-permission android:name="android.permission.INTERNET" />
```

b) gradle引入eclipse.paho.mqtt
```
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0'
```

c) DHT12Thermometer通过I2C读取DHT12数据
```java
private void readDataFromI2C() {

        try {

            byte[] data = new byte[5];
            i2cDevice.readRegBuffer(0x00, data, data.length);

            // check data
            if ((data[0] + data[1] + data[2] + data[3]) % 256 != data[4]) {
                humidity = temperature = 0;
                return;
            }
            // humidity data
            humidity = Double.valueOf(String.valueOf(data[0]) + "." + String.valueOf(data[1]));
            Log.d(TAG, "humidity: " + humidity);
            // temperature data
            if (data[3] < 128) {
                temperature = Double.valueOf(String.valueOf(data[2]) + "." + String.valueOf(data[3]));
            } else {
                temperature = Double.valueOf("-" + String.valueOf(data[2]) + "." + String.valueOf(data[3] - 128));
            }

            Log.d(TAG, "temperature: " + temperature);

        } catch (IOException e) {
            Log.e(TAG, "readDataFromI2C error " + e.getMessage(), e);
        }

    }
```

d) Ze08CH2O通过UART获取ZE08数据
```
try {
                // data buffer
                byte[] buffer = new byte[9];

                while (uartDevice.read(buffer, buffer.length) > 0) {

                    if (checkSum(buffer)) {
                        ppbCh2o = buffer[4] * 256 + buffer[5];
                        ch2o = ppbCh2o / 66.64 * 0.08;
                    } else {
                        ch2o = ppbCh2o = 0;
                    }
                    Log.d(TAG, "ch2o: " + ch2o);
                }

            } catch (IOException e) {
                Log.e(TAG, "Ze08CH2O read data error " + e.getMessage(), e);
            }
```

e) 创建阿里云IoT连接，上报数据
```

/*
payload格式
{
  "id": 123243,
  "params": {
    "temperature": 25.6,
    "humidity": 60.3,
    "ch2o": 0.048
  },
  "method": "thing.event.property.post"
}
*/
MqttMessage message = new MqttMessage(payload.getBytes("utf-8"));
message.setQos(1);

String pubTopic = "/sys/" + productKey + "/" + deviceName + "/thing/event/property/post";

mqttClient.publish(pubTopic, message);

```

### 2.4 设备启动后，在阿里云IoT云端控制台查看实时数据

![](https://raw.githubusercontent.com/iot-blog/aliyun-iot-android-things-nxp/master/images/iot-device-status.png)

### 3. 帮助&反馈

<img src='https://raw.githubusercontent.com/iot-blog/yunqi-iot-demo/master/images/iot-dd.png' width="240" height="300" />

联系我：

<img src='https://raw.githubusercontent.com/wongxming/dtalkNodejs/master/wongxming.jpg' width="240" height="240" />
