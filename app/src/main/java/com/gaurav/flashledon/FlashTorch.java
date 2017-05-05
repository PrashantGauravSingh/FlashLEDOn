package com.gaurav.flashledon;


/**
 * Created by prashant on 05/05/16.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

public class FlashTorch extends Activity {

    private ImageButton btnSwitch;

    private Camera camera;
    private boolean isFlashOn;
    private boolean flashAvailable;
    private Parameters params;
    private MediaPlayer mp;
    private  boolean shakeOn=false;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private shakeDetector mShakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_flash_torch);

        // flash switch button
        btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);

        RateItDialogFragment.show(FlashTorch.this, getFragmentManager());

		/*
		 * First check if device is supporting flashlight or not
		 */
        flashAvailable = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!flashAvailable) {

            AlertDialog alert = new AlertDialog.Builder(FlashTorch.this)
                    .create();
            alert.setTitle("No Support");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }


        getCamera();

        // displaying button image
        toggleButtonImage();

		/*
		 * Switch button click event to toggle flash on/off
		 */
        btnSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    // turn off flash
                    turnOffFlash();
                } else {
                    // turn on flash
                    turnOnFlash();
                }
            }
        });

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new shakeDetector();
        mShakeDetector.setOnShakeListener(new shakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
				/*
				 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 *
				 */if(shakeOn==false){

                    turnOnFlash();
                        shakeOn=true;
                    }else{
                    turnOffFlash();

                    shakeOn=false;
                }

                handleShakeEvent(count);
            }
        });
    }




private void handleShakeEvent(int count) {

    // ignore

    }

    /*
    * Get the camera
    */
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                Log.e(" Error: ", e.getMessage());
            }
        }
    }

    /*
     * Turning On flash
     */
    private void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            // play sound
            playSound();

            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;

            // changing button/switch image
            toggleButtonImage();
        }

    }

    /*
     * Turning Off flash
     */
    private void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            // play sound
            playSound();

            params = camera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;

            // changing button/switch image
            toggleButtonImage();
        }
    }

    /*
     * Playing sound will play button toggle sound on flash on / off
     */
    private void playSound() {
        if (isFlashOn) {
            mp = MediaPlayer.create(FlashTorch.this, R.raw.light_switch_off);
        } else {
            mp = MediaPlayer.create(FlashTorch.this, R.raw.light_switch_on);
        }
        mp.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        });
        mp.start();
    }

    /*
     * Toggle switch button changing image states to on / off
     */
    private void toggleButtonImage() {
        if (isFlashOn) {
            btnSwitch.setImageResource(R.drawable.btn_switch_on);
        } else {
            btnSwitch.setImageResource(R.drawable.btn_switch_off);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        // on pause turn off the flash
        turnOffFlash();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
        // on resume turn on the flash

        if (flashAvailable)
            turnOnFlash();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // on starting the app get the camera params
        getCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public static class RateItDialogFragment extends DialogFragment {
        private static final int LAUNCHES_UNTIL_PROMPT = 10;
        private static final int DAYS_UNTIL_PROMPT = 3;
        private static final int MILLIS_UNTIL_PROMPT = DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000;
        private static final String PREF_NAME = "APP_RATER";
        private static final String LAST_PROMPT = "LAST_PROMPT";
        private static final String LAUNCHES = "LAUNCHES";
        private static final String DISABLED = "DISABLED";

        public  static void show(Context context, FragmentManager fragmentManager) {
            boolean shouldShow = false;
            SharedPreferences sharedPreferences = getSharedPreferences(context);
           SharedPreferences.Editor editor = sharedPreferences.edit();
            long currentTime = System.currentTimeMillis();
            long lastPromptTime = sharedPreferences.getLong(LAST_PROMPT, 0);
            if (lastPromptTime == 0) {
                lastPromptTime = currentTime;
                editor.putLong(LAST_PROMPT, lastPromptTime);
            }

            if (!sharedPreferences.getBoolean(DISABLED, false)) {
                int launches = sharedPreferences.getInt(LAUNCHES, 0) + 1;
                if (launches > LAUNCHES_UNTIL_PROMPT) {
                    if (currentTime > lastPromptTime + MILLIS_UNTIL_PROMPT) {
                        shouldShow = true;
                    }
                }
                editor.putInt(LAUNCHES, launches);
            }

            if (shouldShow) {
                editor.putInt(LAUNCHES, 0).putLong(LAST_PROMPT, System.currentTimeMillis()).commit();
                new RateItDialogFragment().show(fragmentManager, null);
            } else {
                editor.commit();
            }
        }

        private static SharedPreferences getSharedPreferences(Context context) {
            return context.getSharedPreferences(PREF_NAME, 0);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Rate The App")
                    .setMessage(R.string.rate_message)
                    .setPositiveButton("Rate now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName())));
                           getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).commit();
                            dismiss();
                        }
                    })
                    .setNeutralButton("Remind me Later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setNegativeButton("No, Thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).commit();
                            dismiss();
                        }
                    }).create();
        }
    }


}
