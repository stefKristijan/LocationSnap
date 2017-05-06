package hr.ferit.kstefancic.locationsnap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
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
import android.util.StringBuilderPrinter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOError;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int RQT_LOCATION_PERMISSION = 10;
    TextView mTvLocation, mTvAddress;
    LocationListener mLocationListener;
    LocationManager mLocationManager;
    GoogleMap mGoogleMap;
    MapFragment mMapFragment;
    MarkerOptions mCurrentLocationMarker;
    private GoogleMap.OnMapClickListener mCustomOnMapClickListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setUI();
        this.loadSounds();
        this.mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        this.mLocationListener = new SimpleLocationListener();

    }

    private void loadSounds() {
        this.m
    }

    @Override
    protected void onStart() {
        if (!this.hasLocationPermission()) {
            requestPermission();
        }
        super.onStart();

    }


    @Override
    protected void onResume() {
        if (this.hasLocationPermission()) {
            startTracking();
        }
        super.onResume();

    }

    private void startTracking() {
        Log.d("Tracking", "Tracking started");
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String locationProvider = this.mLocationManager.getBestProvider(criteria, true);
        long minTime = 1000;
        float minDistance = 10;
        this.mLocationManager.requestLocationUpdates(locationProvider, minTime, minDistance, this.mLocationListener);
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopTracking();
    }

    private void stopTracking() {
        this.mLocationManager.removeUpdates(this.mLocationListener);
    }

    private void setUI() {
        this.mTvLocation = (TextView) findViewById(R.id.tvLocation);
        this.mTvAddress = (TextView) findViewById(R.id.tvAddress);
        this.mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fGoogleMap);
        this.mMapFragment.getMapAsync(this);
        this.mCustomOnMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round));
                markerOptions.title("I'm here");
                markerOptions.position(latLng);
                mGoogleMap.addMarker(markerOptions);
            }
        };
    }


    private boolean hasLocationPermission() {
        String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        int status = ContextCompat.checkSelfPermission(this, locationPermission);
        if (status == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermission() {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this, permissions, RQT_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RQT_LOCATION_PERMISSION:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permission", "Permission granted the usage of location service");
                    } else {
                        Toast.makeText(this, "You denied access, application closing", Toast.LENGTH_LONG);
                        askForPermission();
                    }
                }
        }
    }

    private void askForPermission() {
        boolean shouldExplain = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldExplain) {
            Log.d("Permission", "Permission should be explained, - don't show again not clicked.");
            this.displayDialog();
        } else {
            Log.d("Permission", "Permission not granted. User pressed deny and don't show again.");
            mTvLocation.setText("Sorry, we really need that permission");
        }
    }

    private void displayDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lat: ").append(location.getLatitude()).append("\n");
        stringBuilder.append("Lon: ").append(location.getLongitude()).append("\n");
        mTvLocation.setText(stringBuilder.toString());
    }

    private void updateLocationText(Location location) {
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            try {
                List<Address> nearbyAddress = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (nearbyAddress.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    Address nearestAddress = nearbyAddress.get(0);
                    stringBuilder.append(nearestAddress.getAddressLine(0)).append("\n")
                            .append(nearestAddress.getLocality()).append("\n")
                            .append(nearestAddress.getCountryName());
                    mTvAddress.setText(stringBuilder.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        UiSettings uiSettings = this.mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);

        this.mGoogleMap.setOnMapClickListener(this.mCustomOnMapClickListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.mGoogleMap.setMyLocationEnabled(true);
    }

    private class SimpleLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            updateLocationDisplay(location);
            updateLocationText(location);
            setMarker(location);
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

    private void setMarker(Location location) {
        mCurrentLocationMarker = new MarkerOptions();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCurrentLocationMarker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round));
        mCurrentLocationMarker.title("I'm here!");
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());


            List<Address> nearbyAddress = null;
            try {
                nearbyAddress = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (nearbyAddress.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    Address nearestAddress = nearbyAddress.get(0);
                    stringBuilder.append(nearestAddress.getAddressLine(0)).append(", ")
                            .append(nearestAddress.getLocality()).append(", ")
                            .append(nearestAddress.getCountryName());
                    mCurrentLocationMarker.snippet(stringBuilder.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mCurrentLocationMarker.position(latLng);
            mGoogleMap.addMarker(mCurrentLocationMarker);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

        }
    }
}
