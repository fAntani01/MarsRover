package com.example.marsrover;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    TextView roverIpTXT;
    TextView roverPortTXT;
    TextView pcIpTXT;
    TextView pcPortTXT;
    TextView timeTXT;
    Button startBtn;

    String roverIp;
    int roverPort;
    String pcIp;
    int pcPort;
    int time;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);



        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        startBtn = findViewById(R.id.button);
        roverIpTXT = findViewById(R.id.roverIP);
        roverPortTXT = findViewById(R.id.roverPort);
        pcIpTXT = findViewById(R.id.computerIP);
        pcPortTXT = findViewById(R.id.computerPort);
        timeTXT = findViewById(R.id.time);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        roverIp = prefs.getString("roverip", "192.168.0.139");
        roverPort = prefs.getInt("roverport", 5000);
        pcIp = prefs.getString("pcip", "192.168.0.160");
        pcPort = prefs.getInt("pcport", 5002);
        time = prefs.getInt("time", 300);

        roverIpTXT.setText(roverIp);
        roverPortTXT.setText(String.valueOf(roverPort));
        pcIpTXT.setText(pcIp);
        pcPortTXT.setText(String.valueOf(pcPort));
        timeTXT.setText(String.valueOf(time));


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences pref = getApplicationContext().getSharedPreferences("settings", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("roverip", roverIpTXT.getText().toString());
                editor.putInt("roverport", Integer.parseInt(roverPortTXT.getText().toString()));
                editor.putString("pcip", pcIpTXT.getText().toString());
                editor.putInt("pcport", Integer.parseInt(pcPortTXT.getText().toString()));
                editor.putInt("time", Integer.parseInt(timeTXT.getText().toString()));
                editor.commit();

                Intent mStartActivity = new Intent(SettingsActivity.this, MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(SettingsActivity.this, mPendingIntentId, mStartActivity,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) SettingsActivity.this.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);


            }
        });


    }
}
