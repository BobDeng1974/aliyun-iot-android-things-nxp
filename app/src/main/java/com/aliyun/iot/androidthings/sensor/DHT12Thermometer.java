package com.aliyun.iot.androidthings.sensor;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class DHT12Thermometer {
    private static final String TAG = DHT12Thermometer.class.getSimpleName();

    private static int DELAY_MILLIS = 3*1000;


    private Handler handler = new Handler();

    private double humidity;
    private double temperature;
    private I2cDevice i2cDevice;

    public double getHumidity() {
        return humidity;
    }

    public double getTemperature() {
        return temperature;
    }

    /*
        i2cDeviceName "I2C1";
        i2sAddress "0x5C";
         */
    public DHT12Thermometer(String i2cDeviceName, int i2sAddress) throws IOException {

        // open I2cDevice
        i2cDevice = PeripheralManager.getInstance().openI2cDevice(i2cDeviceName, i2sAddress);

        handler.postDelayed(() -> readDataFromI2C(), DELAY_MILLIS);
    }

    public void close() {

        if (i2cDevice != null) {
            try {
                i2cDevice.close();
            } catch (IOException e) {
                Log.e(TAG, "DHT12 close error " + e.getMessage(), e);
            } finally {
                i2cDevice = null;
            }
        }
    }

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

        handler.postDelayed(() -> readDataFromI2C(), DELAY_MILLIS);
    }
}
