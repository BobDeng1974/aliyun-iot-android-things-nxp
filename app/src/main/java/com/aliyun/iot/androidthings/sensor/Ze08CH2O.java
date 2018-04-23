package com.aliyun.iot.androidthings.sensor;

import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;

public class Ze08CH2O {
    private static final String TAG = Ze08CH2O.class.getSimpleName();

    private double ch2o;
    private int ppbCh2o;
    private UartDevice uartDevice;

    public double getCh2o() {
        return ch2o;
    }

    public int getPpbCh2o() {
        return ppbCh2o;
    }

    private UartDeviceCallback uartDeviceCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uartDevice) {

            // read data from UART
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

            return true;
        }
    };


    private boolean checkSum(byte[] buffer) {

        int i, temp = 0;

        for (i = 1; i < (buffer.length - 1); i++) {
            temp += buffer[i];
        }

        temp = (~temp) + 1;

        return (temp == buffer[8]);
    }

    public Ze08CH2O(String uartDeviceName) throws IOException {

        // open UART
        uartDevice = PeripheralManager.getInstance().openUartDevice(uartDeviceName);

        // config UART
        uartDevice.setBaudrate(9600);
        uartDevice.setDataSize(8);
        uartDevice.setParity(UartDevice.PARITY_NONE);
        uartDevice.setStopBits(1);

        uartDevice.registerUartDeviceCallback(uartDeviceCallback);

    }

    public void close() {

        if (uartDevice != null) {
            try {
                uartDevice.close();
                uartDevice.unregisterUartDeviceCallback(uartDeviceCallback);
            } catch (IOException e) {
                Log.e(TAG, "Ze08CH2O close error " + e.getMessage(), e);
            } finally {
                uartDevice = null;
            }
        }
    }

}
