package hr.ferit.kstefancic.locationsnap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final int RQT_LOCATION_PERMISSION = 10;
    TextView mTvLocation;
    LocationListener mLocationListener;
    LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setUI();
        this.mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        this.mLocationListener = new SimpleLocationListener();

    }

    @Override
    protected void onStart() {
        if(!this.hasLocationPermission()){
            requestPermission();
        }
        super.onStart();

    }



    @Override
    protected void onResume() {
        if(this.hasLocationPermission())
        {
            //startTracking();
        }
        super.onResume();

    }



    @Override
    protected void onPause() {
        super.onPause();
        //stopTracking();
    }

    private void setUI() {

    }

    private boolean hasLocationPermission(){
        String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        int status = ContextCompat.checkSelfPermission(this,locationPermission);
        if(status == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private void requestPermission(){
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this,permissions,RQT_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case RQT_LOCATION_PERMISSION:
                if(grantResults.length>0){
                    if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                        Log.d("Permission","Permission granted the usage of location service");
                    }
                    else{
                        Toast.makeText(this,"You denied access, application closing",Toast.LENGTH_LONG);
                        askForPermission();
                    }
                }
        }
    }

    private void askForPermission() {
        boolean shouldExplain = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(shouldExplain){
            Log.d("Permission","Permission should be explained, - don't show again not clicked.");
            this.displayDialog();
        }
        else {
            Log.d("Permission", "Permission not granted. User pressed deny and don't show again.");
            mTvLocation.setText("Sorry, we really need that permission");
        }
    }

    private void displayDialog() {
        AlertDialog.Builder dialogBuilder  = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Location permission")
            .setMessage("The application needs your permission for showing your location on the map!")
            .setNegativeButton("Dissmiss", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("Permission", "User denied and won't be asked again.");
                    dialog.cancel();
                }
            })
                .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("Permission", "Permission granted after explanation");
                        requestPermission();
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void updateLocationDisplay(Location location) {
    }

    private class SimpleLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            updateLocationDisplay(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


}
