package hr.ferit.kstefancic.locationsnap;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int RQT_LOCATION_PERMISSION = 10;
    private static final String NO_PERMISSION_TV_MESSAGE = "Unable to read Your location!";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String MSG_KEY = "message";
    private static final String CAPTURE_SUCCESSFULL_TEXT = "Click to view the image!";
    private static final String CAPTURE_SUCCESSFULL_TITLE = "LocationSnap - Image capture successfull!";
    TextView mTvLocation, mTvAddress;
    LocationListener mLocationListener;
    LocationManager mLocationManager;
    GoogleMap mGoogleMap;
    MapFragment mMapFragment;
    MarkerOptions mCurrentLocationMarker;
    private GoogleMap.OnMapClickListener mCustomOnMapClickListener;
    SoundPool mSoundPool;
    boolean mLoaded = false;
    HashMap<Integer,Integer> mSoundMap = new HashMap<>();
    FloatingActionButton fab;
    String mCurrentPhotoPath;
    Uri mImageUri;


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = new SoundPool.Builder().setMaxStreams(10).build();
        }
        else
            this.mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);

        this.mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mLoaded=true;
            }
        });
        this.mSoundMap.put(R.raw.notification,this.mSoundPool.load(this, R.raw.notification,1));
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

        this.fab = (FloatingActionButton) findViewById(R.id.fab);
        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        this.mCustomOnMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.google_location));
                markerOptions.title("Your marker");
                markerOptions.snippet(getAddressFromLocation(latLng));
                markerOptions.position(latLng);
                mGoogleMap.addMarker(markerOptions);
                if(mLoaded)
                    playSound(R.raw.notification);
            }
        };
    }

    private void takePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            if (mCurrentLocationMarker != null) {
                File imageFile = null;
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(imageFile!=null){
                    mImageUri = Uri.fromFile(imageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                    refreshGallery(imageFile);
                }
            } else
                Toast.makeText(this, "The application can't get your current location!", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshGallery(File file) {
        MediaScannerConnection.scanFile(MainActivity.this,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    private File createImageFile() throws IOException {
        LatLng latLng;
        latLng = mCurrentLocationMarker.getPosition();
        String fileName = "picture";
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> nearbyAddress = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (nearbyAddress.size() > 0) {
                    Address nearestAddress = nearbyAddress.get(0);
                    fileName = nearestAddress.getAddressLine(0).replace(" ", "_");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //File imagesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imagesFolder=new File(Environment.getExternalStorageDirectory(),"LocationSnap");
        if(!imagesFolder.exists())
        {
            imagesFolder.mkdirs();
        }
        File image = new File(imagesFolder,fileName+".png");//File.createTempFile(fileName, ".png", imagesFolder);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK){
            sendNotification();
        }
    }

    private void sendNotification() {
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, mImageUri);
        notificationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        notificationIntent.setDataAndType(mImageUri,"image/*");
        notificationIntent.putExtra(MSG_KEY, CAPTURE_SUCCESSFULL_TEXT);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this,0,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setAutoCancel(true)
                .setContentTitle(CAPTURE_SUCCESSFULL_TITLE)
                .setContentText(CAPTURE_SUCCESSFULL_TEXT)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(notificationPendingIntent)
                .setLights(Color.BLUE,1000,1000)
                .setVibrate(new long[]{1000,1000,1000,1000,1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Notification notification = notificationBuilder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,notification);
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
            this.displayDialog();
        } else {
            mTvLocation.setText(NO_PERMISSION_TV_MESSAGE);
            mTvAddress.setText(NO_PERMISSION_TV_MESSAGE);
        }
    }

    private void displayDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Location permission")
                .setMessage("The application needs your permission for showing your location on the map!")
                .setNegativeButton("Dissmiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermission();
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void updateLocationDisplay(Location location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Latitude: ").append(location.getLatitude()).append("\n");
        stringBuilder.append("Longitude: ").append(location.getLongitude()).append("\n");
        mTvLocation.setText(stringBuilder.toString());
    }

    private String getAddressFromLocation (LatLng latLng){

        String address=null;
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            try {
                List<Address> nearbyAddress = geocoder.getFromLocation(latLng.latitude,latLng.longitude, 1);
                if (nearbyAddress.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    Address nearestAddress = nearbyAddress.get(0);
                    stringBuilder.append(nearestAddress.getAddressLine(0)).append(", ")
                            .append(nearestAddress.getLocality()).append(", ")
                            .append(nearestAddress.getCountryName());
                    address = stringBuilder.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            return address;
    }

    private void updateLocationText(Location location) {
       mTvAddress.setText(getAddressFromLocation(new LatLng(location.getLatitude(),location.getLongitude())));
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
        mCurrentLocationMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location));
        mCurrentLocationMarker.title("I'm here!");
        mCurrentLocationMarker.snippet(getAddressFromLocation(new LatLng(location.getLatitude(),location.getLongitude())));
        mCurrentLocationMarker.position(latLng);
        mGoogleMap.addMarker(mCurrentLocationMarker);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
    }

    private void playSound(int notification) {
        int soundID = this.mSoundMap.get(notification);
        this.mSoundPool.play(soundID,1,1,1,0,1f);
    }
}
