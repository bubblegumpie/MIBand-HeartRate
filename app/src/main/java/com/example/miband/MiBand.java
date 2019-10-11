package com.example.miband;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class MiBand {

    private Activity activity;
    private Timer ping;
    private BluetoothDevice device;
    private BluetoothGattCallback miBandGattCallBack;
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private ScanCallback leDeviceScanCallback;
    private boolean connected;
    private String status;
    private ArrayList<Integer> heartRateValues;




    public MiBand(Activity activity) {
        this.activity = activity;
        getPermissions();

        status = "Disconnected";
        heartRateValues = new ArrayList<>();
        createCallback();
    }

    public String getStatus() {
        return status;
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
        status = "Disconnected";
    }

    public boolean connect() {
        if (!connected) {
            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice d : pairedDevices) {
                    String deviceName = d.getName();
                    String macAddress = d.getAddress();
                    if (deviceName.toLowerCase().contains("band")) {
                        device = d;
                        device.createBond();

                        ping = new Timer(5000, () -> {
                            if (!connected)
                                bluetoothGatt = device.connectGatt(activity.getApplicationContext(), true
                                        , miBandGattCallBack, BluetoothDevice.TRANSPORT_LE);
                            else
                                ping.stop();
                        });
                        ping.start();
                        return true;
                    }
                }
            }
            bluetoothAdapter = ((BluetoothManager) activity.getSystemService(activity.BLUETOOTH_SERVICE)).getAdapter();

            leDeviceScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    String deviceName = result.getDevice().getName();
                    if (deviceName != null) {
                        if (deviceName.toLowerCase().contains("band")) {
                            bluetoothAdapter.getBluetoothLeScanner().stopScan(leDeviceScanCallback);
                            device = result.getDevice();

                            device.createBond();
                            status = "Bonded";

                            bluetoothGatt = device.connectGatt(activity.getApplicationContext(), true
                                    , miBandGattCallBack, BluetoothDevice.TRANSPORT_LE);
                            ping = new Timer(5000, () -> {
                                if (!connected)
                                    bluetoothGatt = device.connectGatt(activity.getApplicationContext(), true
                                            , miBandGattCallBack, BluetoothDevice.TRANSPORT_LE);
                                else
                                    ping.stop();
                            });
                            ping.start();

                        }
                    }
                }
            };
            bluetoothAdapter.getBluetoothLeScanner().startScan(leDeviceScanCallback);
        }
        return true;
    }

    public boolean isConnectted() {
        return connected;
    }

    public ArrayList<Integer> getHeartRateValues(){
        return heartRateValues;
    }

    private void createCallback() {
        miBandGattCallBack = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        gatt.discoverServices();
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (!connected) {
                    MiBand.this.status = "Authenticating";
                    connected = true;
                    startHeartRateContiniousScan();
                    MiBand.this.status = "Online";
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte x = (byte) (characteristic.getValue()[1] & 0xff);
                int value = x;
                if(x < 0)
                    value += 256;

                heartRateValues.add(value);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                BluetoothGattCharacteristic hmc = bluetoothGatt.getService(UUIDs.HEART_RATE_SERVICE).getCharacteristic(UUIDs.HEART_RATE_CONTROL_POINT_CHARACTERISTIC);
                hmc.setValue(new byte[]{1, 1});
                bluetoothGatt.writeCharacteristic(hmc);
            }
        };
    }

    public void startHeartRateContiniousScan() {
        BluetoothGattCharacteristic hmc = bluetoothGatt.getService(UUIDs.HEART_RATE_SERVICE).getCharacteristic(UUIDs.HEART_RATE_CONTROL_POINT_CHARACTERISTIC);

        BluetoothGattCharacteristic hrm = bluetoothGatt.getService(UUIDs.HEART_RATE_SERVICE).getCharacteristic(UUIDs.HEART_RATE_MEASUREMENT_CHARACTERISTIC);
        BluetoothGattDescriptor descriptor = hrm.getDescriptor(UUIDs.HEART_RATE_MEASURMENT_DESCRIPTOR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.setCharacteristicNotification(hrm, true);

        //not necessary
        ping = new Timer(12000, () -> { //ping every twelve seconds
            hmc.setValue(new byte[]{0x16});
            bluetoothGatt.writeCharacteristic(hmc);
        });
        ping.start();

    }

    private void getPermissions() {
        if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED
                    || ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission_group.LOCATION) == PackageManager.PERMISSION_DENIED)
                activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH
                        , Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
}
