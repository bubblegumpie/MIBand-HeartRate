package com.example.miband;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MiBand band;
    private static final String TAG = "HEART RATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        band = new MiBand(this);
        band.connect();

        new Timer(10000, () -> band.getHeartRateValues().stream().forEach(x -> Log.d(TAG,"" + x)));
    }

    @Override
    protected void onDestroy() {
        band.disconnect();
        super.onDestroy();
    }
}
