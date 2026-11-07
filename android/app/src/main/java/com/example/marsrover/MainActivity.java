package com.example.marsrover;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.freedesktop.gstreamer.GStreamer;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private native void nativeInit();     // Initialize native code, build pipeline, etc

    private native void nativeFinalize(); // Destroy pipeline and shutdown native code

    private native void nativePlay();     // Set pipeline to PLAYING

    private native void nativePause();    // Set pipeline to PAUSED

    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks

    private native void nativeSurfaceInit(Object surface1, Object surface2);

    private native void nativeSurfaceFinalize();

    private long native_custom_data;      // Native code will use this to keep private data

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING



    SurfaceView sv1;
    SurfaceView sv2;


    Object srf1;
    Object srf2;

    TextView infoTxt1;
    TextView infoTxt2;
    TextView timerTxt1;
    TextView timerTxt2;
    LinearLayout linearLayout;

    MediaPlayer mp;


    private SensorManager mSensorManager = null;

    // angular speeds from gyro
    private float[] gyro = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magnet = new float[3];

    // accelerometer vector
    private float[] accel = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public static final int TIME_CONSTANT = 50;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    // The following members are only for displaying the sensor output.
    public Handler mHandler;
    DecimalFormat d = new DecimalFormat("#.##");
    DecimalFormat d1 = new DecimalFormat("###");
    NumberFormat f = new DecimalFormat("00");


    UdpClient piUdpClient = null;
    UdpClient pcUdpClient = null;

    double tx;
    double ty;
    int stream = 0;
    double Mx1;
    double Mx2;
    int count =0;


    double valuex;
    double valuey;
    String move = "000";
    String steer = "000";
    String direction = "0";
    boolean led;
    boolean em;

    int time = 60;

    boolean init;


    String piAddress = "192.168.0.139";
    int piPort= 5000;
    String pcAddress = "192.168.0.160";
    int pcPort = 5002;







    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |  View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        Log.d ("ANTANI", width + "x" + height);


        setContentView(R.layout.activity_main);


        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        piAddress = prefs.getString("roverip", "192.168.0.139");
        piPort = prefs.getInt("roverport", 5000);
        pcAddress = prefs.getString("pcip", "192.168.0.160");
        pcPort = prefs.getInt("pcport", 5002);
        time = prefs.getInt("time", 300);



        sv1 = (GStreamerSurfaceView) this.findViewById(R.id.surface_video1);
        sv2 = (GStreamerSurfaceView) this.findViewById(R.id.surface_video2);

        infoTxt1 = (TextView)findViewById(R.id.infoTxt1);
        infoTxt2 = (TextView)findViewById(R.id.infoTxt2);
        timerTxt1 = (TextView)findViewById(R.id.timerTxt1);
        timerTxt2 = (TextView)findViewById(R.id.timerTxt2);
        linearLayout = (LinearLayout)findViewById(R.id.linearLayout);




        infoTxt1.setText("TOCCA PER INIZIARE LA CALIBRAZIONE");
        infoTxt2.setText("TOCCA PER INIZIARE LA CALIBRAZIONE");



        new piConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new pcConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);




        
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (stream == 0){

                    stream = 1;

                    infoTxt1.setText("CALIBRAZIONE IN CORSO...\nNON MUOVERE IL VISORE");
                    infoTxt2.setText("CALIBRAZIONE IN CORSO...\nNON MUOVERE IL VISORE");

                }



                if (stream==2) {

                    stream = 3;
                    infoTxt1.setText("");
                    infoTxt2.setText("");
                    if (!init){
                        nativeInit();
                        nativeSurfaceInit(srf1, srf2);
                        init=true;
                    }
                    infoTxt1.setVisibility(View.INVISIBLE);
                    infoTxt2.setVisibility(View.INVISIBLE);
                    nativePlay();
                    mp = MediaPlayer.create(getApplicationContext(), R.raw.soundtrack);
                    mp.start();
                    if (pcUdpClient != null) {

                        pcUdpClient.sendMessage("start");

                    }



                    new CountDownTimer(time*1000, 1000) {

                        public void onTick(long millisUntilFinished) {

                            //String millis = String.valueOf(millisUntilFinished % 1000).substring(0,2);
                            long second = (millisUntilFinished / 1000) % 60;
                            long minute = (millisUntilFinished / (1000 * 60)) % 60;


                            String time = f.format(minute) + ":" + f.format(second);
                            timerTxt1.setText(time);
                            timerTxt2.setText(time);

                            if (minute == 0 && second <=10){

                                timerTxt1.setTextColor(Color.RED);
                                timerTxt2.setTextColor(Color.RED);

                            }

                        }

                        public void onFinish() {

                            nativePause();
                            timerTxt1.setText("00:00");
                            timerTxt2.setText("00:00");
                            infoTxt1.setText("TOCCA PER INIZIARE LA CALIBRAZIONE");
                            infoTxt2.setText("TOCCA PER INIZIARE LA CALIBRAZIONE");
                            timerTxt1.setTextColor(Color.WHITE);
                            timerTxt2.setTextColor(Color.WHITE);
                            timerTxt1.setText("");
                            timerTxt2.setText("");
                            infoTxt1.setVisibility(View.VISIBLE);
                            infoTxt2.setVisibility(View.VISIBLE);
                            mp.stop();
                            if (pcUdpClient != null) {

                                pcUdpClient.sendMessage("stop");

                            }
                            stream = 0;

                        }
                    }.start();


                }






            }
        });




        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }





        SurfaceHolder sh1 = sv1.getHolder();


        sh1.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                srf1= holder.getSurface();

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

               srf1= holder.getSurface();

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        }) ;






        SurfaceHolder sh2 = sv2.getHolder();
        sh2.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                srf2= holder.getSurface();


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                srf2= holder.getSurface();

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        }) ;












        if (savedInstanceState != null) {
            is_playing_desired = savedInstanceState.getBoolean("playing");
            Log.i("GStreamer", "Activity created. Saved state is playing:" + is_playing_desired);
        } else {
            is_playing_desired = false;
            Log.i("GStreamer", "Activity created. There is no saved state, playing: false");
        }

        // Start with disabled buttons, until native code is initialized






        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();

        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);

        // GUI stuff
        mHandler = new Handler();
        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);



        


    }



    public void settings(View v){

        Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(i);
        this.finish();

    }



    @Override
    public void onStop() {
        super.onStop();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }



    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // restore the sensor listeners when user resumes the application.
        initListeners();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("GStreamer", "Saving state, playing:" + is_playing_desired);
        outState.putBoolean("playing", is_playing_desired);
        super.onSaveInstanceState(outState);
    }




    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        float xaxis = event.getAxisValue(MotionEvent.AXIS_X);

        float frwdAxis = event.getAxisValue(MotionEvent.AXIS_RY);

        float bckwrdAxis = event.getAxisValue(MotionEvent.AXIS_RX);


        if (frwdAxis != -1 && bckwrdAxis == -1){

            move = d1.format((255/2) * frwdAxis + (255/2));

        }

        else if (frwdAxis == -1 && bckwrdAxis != -1){

            move = d1.format((255/2) * bckwrdAxis + (767/2));

        }

        else if (frwdAxis == -1 & bckwrdAxis == -1){

            move = "000";

        }

        if (move.length()==1) move = "00" + move;

        if (move.length()==2) move = "0" + move;

        steer = d1.format(110*xaxis + 390);




        float xaxis1 = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float yaxis = event.getAxisValue(MotionEvent.AXIS_HAT_Y);


        if (xaxis1 == 1){

            direction = "r";

        }

        else if (xaxis1 == -1){

            direction = "l";

        }

        else if (yaxis == 1){

            direction = "b";

        }

        else if (yaxis == -1){

            direction = "f";

        }

        else if (xaxis1 == 0 || yaxis == 0){

            direction = "0";

        }



        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_Z:
            {

                direction = "u";

                return true;
            }

            case KeyEvent.KEYCODE_BUTTON_Y:
            {

                direction = "d";

                return true;
            }

            case KeyEvent.KEYCODE_BUTTON_B:
            {

                led = !led;

                return true;
            }

            case KeyEvent.KEYCODE_BUTTON_X:
            {

                em = !em;

                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {



        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_Z:
            {

                direction = "0";

                return true;
            }

            case KeyEvent.KEYCODE_BUTTON_Y:
            {

                direction = "0";

                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }






    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {

        //runOnUiThread(new Runnable() {
        //    public void run() {
        //        tv.setText(message);
        //    }
        //});
    }





    public void initListeners(){

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
    }





    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array and calculate orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }
    }



    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }



    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }




    class calculateFusedOrientationTask extends TimerTask {




        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;

            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;

            }
            else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);


            // update sensor output in GUI
            mHandler.post(updateOreintationDisplayTask);
        }
    }


    // **************************** GUI FUNCTIONS *********************************


    public void updateOreintationDisplay() {

        //infoTxt1.setText(d1.format(steer));


        double x = fusedOrientation[0] * 180/Math.PI;
        double y = fusedOrientation[2] * 180/Math.PI;

        if (x<0) x=360+x;
        if (y<0) y=360+y;


        if (stream==1){


            if (count > 0 && count < 50) Mx1 +=x;

            if (count > 50 && count < 100) Mx2 +=x;

            //infoTxt1.setText(d.format(x)+ "\n\n"+ d.format(Mx1) + "\n\n" + String.valueOf(count));
            //infoTxt2.setText(d.format(x));



            if (count == 100){


                //Log.d("aaa", String.valueOf(Mx1/49) + "\n" + String.valueOf(Mx2/49));

                if (Math.abs((Mx1/49)-(Mx2/49)) <= 2){

                    infoTxt1.setText("TOCCA PER INIZIARE");
                    infoTxt2.setText("TOCCA PER INIZIARE");

                    stream = 2;

                }

                count=0;
                Mx1=0;
                Mx2=0;
            }



            count = count +1;

        }




        else if (stream == 3){

            tx = 60 - x;
            ty = 60 - y;

            //Toast.makeText(getApplicationContext(), String.valueOf(tx), Toast.LENGTH_SHORT).show();

            stream = 4;

        }


        else if (stream == 4){




            double xa = x+tx;

            if (xa>=360){
                xa = xa-360;
            }
            else if (xa<0){
                xa = xa+360;
            }



            if (0 <= (xa) && (xa) <=120) valuex = -3*(xa) + 580;
            if (45 <= (y+ty) && (y+ty) <=90) valuey = 2.666*(y + ty) + 230;

            int ledI = led ? 1 : 0;
            int emI = em ? 1 : 0;

            if (piUdpClient != null) {

                //piUdpClient.sendMessage("b" + d1.format(valuex) + d1.format(valuey) + steer + move + String.valueOf(ledI) + direction + String.valueOf(emI)+ "e");
                piUdpClient.sendMessage("c" + d1.format(valuex) + d1.format(valuey) + "000f");


            }



        }







    }






    private Runnable updateOreintationDisplayTask = new Runnable() {
        public void run() {
            updateOreintationDisplay();
        }
    };







    public class piConnectTask extends AsyncTask<String, String, UdpClient> {

        @Override
        protected UdpClient doInBackground(String... message) {



            piUdpClient = new UdpClient(piAddress, piPort, new UdpClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {

                }
            });
            piUdpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

        }
    }

    public class pcConnectTask extends AsyncTask<String, String, UdpClient> {

        @Override
        protected UdpClient doInBackground(String... message) {



            pcUdpClient = new UdpClient(pcAddress, pcPort, new UdpClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {

                }
            });
            pcUdpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

        }
    }
    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized() {





        Log.i("GStreamer", "Gst initialized. Restoring state, playing:" + is_playing_desired);
        // Restore previous playing state
        nativePlay();

        // Re-enable buttons, now that GStreamer is initialized
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            public void run() {

            }
        });





    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
    }



}
