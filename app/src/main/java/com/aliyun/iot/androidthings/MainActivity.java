package com.aliyun.iot.androidthings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.aliyun.iot.androidthings.sensor.DHT12Thermometer;
import com.aliyun.iot.androidthings.sensor.Ze08CH2O;
import com.aliyun.iot.androidthings.util.AliyunIoTSignUtil;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String uartDeviceName = "UART6";
    private Ze08CH2O ze08Ch2o;

    private static final String i2cDeviceName = "I2C1";
    private static final int i2sAddress = 0x5C;
    private DHT12Thermometer thermometer;

    private DecimalFormat df = new DecimalFormat("0.0##");

    private static final int DELAY_MILLIS = 5 * 1000;

    public static String productKey = "替换自己的产品key";
    public static String deviceName = "替换自己的产品deviceName";
    public static String deviceSecret = "替换自己的产品secret";

    //property post topic
    private static String pubTopic = "/sys/" + productKey + "/" + deviceName + "/thing/event/property/post";

    private static final String payloadJson = "{\"id\":%s,\"params\":{\"temperature\": %s,\"humidity\": %s,\"ch2o\": %s},\"method\":\"thing.event.property.post\"}";

    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView ch2oTextView;

    private Handler handler = new Handler();
    private MqttClient mqttClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        ch2oTextView = findViewById(R.id.ch2oTextView);

        initSensors();

        initAliyunIoTClient();

        handler.postDelayed(() -> postDeviceProperties(), DELAY_MILLIS);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (thermometer != null) {
            thermometer.close();
        }
        if (ze08Ch2o != null) {
            ze08Ch2o.close();
        }
    }

    private void initSensors() {
        try {
            thermometer = new DHT12Thermometer(i2cDeviceName, i2sAddress);

        } catch (Exception e) {
            Log.e(TAG, "init DHT12 error " + e.getMessage(), e);
        }
        try {
            ze08Ch2o = new Ze08CH2O(uartDeviceName);

        } catch (Exception e) {
            Log.e(TAG, "init ZE08 error " + e.getMessage(), e);
        }
    }


    private void initAliyunIoTClient() {

        try {
            String clientId = "androidthings" + System.currentTimeMillis();

            Map<String, String> params = new HashMap<String, String>(16);
            params.put("productKey", productKey);
            params.put("deviceName", deviceName);
            params.put("clientId", clientId);
            String timestamp = String.valueOf(System.currentTimeMillis());
            params.put("timestamp", timestamp);

            // cn-shanghai
            String targetServer = "tcp://" + productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

            String mqttclientId = clientId + "|securemode=3,signmethod=hmacsha1,timestamp=" + timestamp + "|";
            String mqttUsername = deviceName + "&" + productKey;
            String mqttPassword = AliyunIoTSignUtil.sign(params, deviceSecret, "hmacsha1");

            connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);

        } catch (Exception e) {
            Log.e(TAG, "initAliyunIoTClient error " + e.getMessage(), e);
        }
    }

    public void connectMqtt(String url, String clientId, String mqttUsername, String mqttPassword) throws Exception {

        MemoryPersistence persistence = new MemoryPersistence();
        mqttClient = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        // MQTT 3.1.1
        connOpts.setMqttVersion(4);
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(60);

        mqttClient.connect(connOpts);
        Log.d(TAG, "connected " + url);

    }


    private void postDeviceProperties() {

        try {
            if (thermometer != null) {
                temperatureTextView.setText("Temperature: " + thermometer.getTemperature() + " ℃");
                humidityTextView.setText("Humidity: " + thermometer.getHumidity() + " %");
            }
            if (ze08Ch2o != null) {
                ch2oTextView.setText("HCHO: " + df.format(ze08Ch2o.getCh2o()) + " mg/m3");
            }


            //上报数据
            String payload = String.format(payloadJson, String.valueOf(System.currentTimeMillis()), df.format(thermometer.getTemperature()), df.format(thermometer.getHumidity()), df.format(ze08Ch2o.getCh2o()));

            MqttMessage message = new MqttMessage(payload.getBytes("utf-8"));
            message.setQos(1);

            if(mqttClient == null){
                initAliyunIoTClient();
            }else{
                mqttClient.publish(pubTopic, message);
                Log.d(TAG, "publish topic=" + pubTopic + ",payload=" + payload);
            }

        } catch (Exception e) {
            Log.e(TAG, "postDeviceProperties error " + e.getMessage(), e);
        }

        handler.postDelayed(() -> postDeviceProperties(), DELAY_MILLIS);
    }

}
