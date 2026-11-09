package com.example.assignment_2_tracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounter, accelerometer;

    private ActivityAdapter adapter;
    private DBHelper dbHelper;

    private float lastCounter = -1f;
    private float baseSteps = -1f;
    private int currentSteps = 0, currentShakes = 0;
    private long startTime, lastShakeTime = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());


    private TextView textCurrentSteps, textCurrentShakes, textDuration, textStepsPerMin,
            textShakesPerMin, textTotalSteps, textTotalShakes, textNoHistory;

    private RecyclerView recyclerHistory;
    private Button buttonSave;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private void bindViews() {
        textCurrentSteps = findViewById(R.id.text_current_steps);
        textCurrentShakes = findViewById(R.id.text_current_shakes);
        textDuration = findViewById(R.id.text_duration);
        textStepsPerMin = findViewById(R.id.text_steps_per_min);
        textShakesPerMin = findViewById(R.id.text_shakes_per_min);
        textTotalSteps = findViewById(R.id.text_total_steps);
        textTotalShakes = findViewById(R.id.text_total_shakes);
        textNoHistory = findViewById(R.id.text_no_history);

        recyclerHistory = findViewById(R.id.recycler_history);

        buttonSave = findViewById(R.id.button_save);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        bindViews();
        discoverSensors();


        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(this::showDetail);
        recyclerHistory.setAdapter(adapter);

        startNewSession();
        refreshHistory();

        if (stepCounter == null)
            Toast.makeText(this, R.string.no_step_counter, Toast.LENGTH_LONG).show();

        buttonSave.setOnClickListener(v -> saveSession());
    }


    private final ActivityResultLauncher<String> activityRecognitionPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) registerSensors();
                else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            });

    private void discoverSensors() {
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
            activityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            registerSensors();
        }
    }

    private boolean hasActivityPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void registerSensors() {
        if (stepCounter != null && hasActivityPermission()) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (accelerometer != null) {    // ไม่ต้องใช้สิทธิ์
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }



    @Override protected void onResume() {
        super.onResume();
        registerSensors();
        handler.post(ticker);
    }

    @Override protected void onPause() {
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            lastCounter = e.values[0];
            if (baseSteps < 0) baseSteps = lastCounter;
            currentSteps = Math.max(0, Math.round(lastCounter - baseSteps));
            textCurrentSteps.setText(String.valueOf(currentSteps));
        } else if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(e.values);
        }
    }

    private void detectShake(float[] v) {
        double mag = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        long now = System.currentTimeMillis();
        if (Math.abs(mag - SensorManager.GRAVITY_EARTH) > 3.2 && now - lastShakeTime > 700) {
            lastShakeTime = now;
            currentShakes++;
            textCurrentShakes.setText(String.valueOf(currentShakes));
        }
    }

    @Override public void onAccuracyChanged(Sensor s, int a) { }



    @SuppressLint("SetTextI18n")
    private void startNewSession() {
        startTime = System.currentTimeMillis();
        baseSteps = lastCounter;
        currentSteps = 0;
        currentShakes = 0;
        textCurrentSteps.setText("0");
        textCurrentShakes.setText("0");
        textDuration.setText("00:00");
        textStepsPerMin.setText("0.0");
        textShakesPerMin.setText("0.0");
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long sec = (System.currentTimeMillis() - startTime) / 1000;
            double min = Math.max(sec / 60.0, 1.0 / 60);
            textDuration.setText(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60));
            textStepsPerMin.setText(String.format(Locale.US, "%.1f", currentSteps / min));
            textShakesPerMin.setText(String.format(Locale.US, "%.1f", currentShakes / min));
            handler.postDelayed(this, 1000);
        }
    };

    private void saveSession() {
        int durationSec = (int) ((System.currentTimeMillis() - startTime) / 1000);
        dbHelper.insertActivity(startTime, currentSteps, currentShakes, durationSec);
        startNewSession();
        refreshHistory();
        recyclerHistory.scrollToPosition(0);
    }

    private void refreshHistory() {
        List<ActivityItem> list = dbHelper.getAllActivities();
        adapter.setData(list);
        boolean empty = list.isEmpty();
        recyclerHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
        textNoHistory.setVisibility(empty ? View.VISIBLE : View.GONE);

        int totalSteps = dbHelper.getTotalSteps();
        int totalShakes = dbHelper.getTotalShakes();

        textTotalSteps.setText(getString(R.string.total_steps_format, totalSteps));
        textTotalShakes.setText(getString(R.string.total_shakes_format, totalShakes));
    }

    private void showDetail(ActivityItem a) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_activity_detail, null);

        ((TextView) view.findViewById(R.id.text_detail_date)).setText(
                dateFormat.format(new Date(a.getStartTime())));
        ((TextView) view.findViewById(R.id.text_detail_steps)).setText(String.valueOf(a.getSteps()));
        ((TextView) view.findViewById(R.id.text_detail_shakes)).setText(String.valueOf(a.getShakes()));
        ((TextView) view.findViewById(R.id.text_detail_duration)).setText(
                String.format(Locale.US, "%02d:%02d", a.getDurationSec() / 60, a.getDurationSec() % 60));
        ((TextView) view.findViewById(R.id.text_detail_steps_per_min)).setText(
                String.format(Locale.US, "%.1f", a.getStepsPerMin()));
        ((TextView) view.findViewById(R.id.text_detail_shakes_per_min)).setText(
                String.format(Locale.US, "%.1f", a.getShakesPerMin()));

        view.findViewById(R.id.card_new_step_record)
                .setVisibility(a.getStepRecord() ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.card_new_shake_record)
                .setVisibility(a.getShakeRecord() ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        view.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.button_delete).setOnClickListener(v -> {
            if (dbHelper.deleteActivity(a.getId()) > 0) {
                dialog.dismiss();
                refreshHistory();
            } else {
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }



    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}