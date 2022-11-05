package com.example.testingseniorproject1;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomePage extends AppCompatActivity implements View.OnClickListener {

    ImageView menu;
    Button button1, button2, button3, button4, button5, button6, button7, button8, showEscooters;
    private boolean IsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        //----------------------------------------------------------------------------------------------------
        menu = (ImageView) findViewById(R.id.menu);
        menu.setOnClickListener(this);
        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(this);
        button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(this);
        button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(this);
        button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(this);
        button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(this);
        button6 = (Button) findViewById(R.id.button6);
        button6.setOnClickListener(this);
        button7 = (Button) findViewById(R.id.button7);
        button7.setOnClickListener(this);
        button8 = (Button) findViewById(R.id.button8);
        button8.setOnClickListener(this);
        showEscooters = (Button) findViewById(R.id.showEscooters);
        showEscooters.setOnClickListener(this);

        progressDialog = new ProgressDialog(HomePage.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Checking availability...");

        databaseReference()
                .child(ESCOOTERS)
                .removeValue();

        setUpMap();
    }

    private void setUpMap() {
        // Try to obtain the map from the SupportMapFragment.
        SupportMapFragment mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                map = googleMap;
                if (ActivityCompat.checkSelfPermission(HomePage.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(HomePage.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // We are calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission.
                    ActivityCompat.requestPermissions(HomePage.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
                    return;
                }
                googleMap.setMyLocationEnabled(true);
                googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location arg0) {
                        myLocation = arg0;
                        if (shouldMapMove) {
                            LatLng myLatLng = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                        }
                    }
                });

                googleMap.setOnMarkerClickListener(marker -> {
                    progressDialog.show();
                    // CHECKING IF AN ESCOOTER IS ALREADY BOOKED
                    databaseReference().child(ESCOOTERS).child(marker.getSnippet())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    progressDialog.dismiss();
                                    if (snapshot.exists()) {
                                        // THIS ESCOOTER IS ALREADY BOOKED
                                        Toast.makeText(HomePage.this, "This is already booked!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // SETTING STATUS TO BOOKED IN DB
                                        databaseReference()
                                                .child(ESCOOTERS)
                                                .child(marker.getSnippet())
                                                .setValue(true);

                                        showCustomDialog();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    progressDialog.dismiss();
                                    Toast.makeText(HomePage.this, error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    return false;
                });
            }
        });
    }

    // THIS IS SHOWING A DIALOG WHICH WILL GET USER DATE AND TIME FOR RENT
    private void showCustomDialog() {
        Dialog dialog = new Dialog(HomePage.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog);
        dialog.setCancelable(false);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        RadioGroup radioGroup = dialog.findViewById(R.id.radioGroup);

        dialog.findViewById(R.id.submitBtn).setOnClickListener(view -> {
            RadioButton radioButton = dialog.findViewById(radioGroup.getCheckedRadioButtonId());

            if (currentSelectedDateStr == null) {
                Toast.makeText(this, "Please select date!", Toast.LENGTH_SHORT).show();
                return;
            }

            currentSelectedTimeInt = Integer.parseInt(radioButton.getTag().toString());

            startActivity(new Intent(HomePage.this, PaymentActivity.class)
                    .putExtra("date", currentSelectedDateStr)
                    .putExtra("time", currentSelectedTimeInt)
            );

        });

        dialog.findViewById(R.id.selectDate).setOnClickListener(v -> {
            MaterialDatePicker.Builder materialDateBuilder = MaterialDatePicker.Builder.datePicker();
            // now define the properties of the
            // materialDateBuilder that is title text as SELECT A DATE
            materialDateBuilder.setTitleText("SELECT A DATE");

            // now create the instance of the material date
            // picker
            final MaterialDatePicker materialDatePicker = materialDateBuilder.build();
            materialDatePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");

            // now handle the positive button click from the
            // material design date picker
            materialDatePicker.addOnPositiveButtonClickListener(
                    selection -> {
                        // if the user clicks on the positive
                        // button that is ok button update the
                        // selected date
                        TextView textView = dialog.findViewById(R.id.selectDateTv);
                        textView.setText(materialDatePicker.getHeaderText());
                        currentSelectedDateStr = materialDatePicker.getHeaderText();
                        // in the above statement, getHeaderText
                        // is the selected date preview from the
                        // dialog
                    });
        });

        dialog.show();
        dialog.getWindow().setAttributes(layoutParams);
    }

    private DatabaseReference databaseReference() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.keepSynced(true);
        return db;
    }

    String ESCOOTERS = "escooters";
    boolean shouldMapMove = true;
    Location myLocation;
    GoogleMap map;
    private ProgressDialog progressDialog;
    String currentSelectedDateStr = null;
    int currentSelectedTimeInt = 5;

    /**
     * Create a new location specified in meters and bearing from a previous location.
     *
     * @param startLoc from where
     * @param bearing  which direction, in radians from north
     * @param distance meters from startLoc
     * @return a new location
     */
    public Location createLocation(Location startLoc, double bearing, double distance) {
        Location newLocation = new Location("newLocation");

        double radius = 6371000.0; // earth's mean radius in m
        double lat1 = Math.toRadians(startLoc.getLatitude());
        double lng1 = Math.toRadians(startLoc.getLongitude());
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / radius) + Math.cos(lat1) * Math.sin(distance / radius) * Math.cos(bearing));
        double lng2 = lng1 + Math.atan2(Math.sin(bearing) * Math.sin(distance / radius) * Math.cos(lat1), Math.cos(distance / radius) - Math.sin(lat1) * Math.sin(lat2));
        lng2 = (lng2 + Math.PI) % (2 * Math.PI) - Math.PI;

        // normalize to -180...+180
        if (lat2 == 0 || lng2 == 0) {
            newLocation.setLatitude(0.0);
            newLocation.setLongitude(0.0);
        } else {
            newLocation.setLatitude(Math.toDegrees(lat2));
            newLocation.setLongitude(Math.toDegrees(lng2));
        }

        return newLocation;
    }

    // THIS IS RE OPENING THE APP AGAIN FOR A REFRESH
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate();
        }
    }

    @Override
    public void onClick(View view) {
        //------------------------------------------------------------------------------------------
        //Show Menu
        if (view.getId() == R.id.menu) {
            if (IsVisible == false) {
                button1.setVisibility(View.VISIBLE);
                button2.setVisibility(View.VISIBLE);
                button3.setVisibility(View.VISIBLE);
                button4.setVisibility(View.VISIBLE);
                button5.setVisibility(View.VISIBLE);
                button6.setVisibility(View.VISIBLE);
                button7.setVisibility(View.VISIBLE);
                button8.setVisibility(View.VISIBLE);
                IsVisible = true;
            } else if (IsVisible == true) {
                button1.setVisibility(View.INVISIBLE);
                button2.setVisibility(View.INVISIBLE);
                button3.setVisibility(View.INVISIBLE);
                button4.setVisibility(View.INVISIBLE);
                button5.setVisibility(View.INVISIBLE);
                button6.setVisibility(View.INVISIBLE);
                button7.setVisibility(View.INVISIBLE);
                button8.setVisibility(View.INVISIBLE);
                IsVisible = false;
            }
        }
        //------------------------------------------------------------------------------------------
        //
        switch (view.getId()) {
            case R.id.button1://My Account
                startActivity(new Intent(this, MyAccount.class));
                break;

            case R.id.button2://Ride History
                startActivity(new Intent(this, RideHistory.class));
                break;

            case R.id.button3://Wallet
                startActivity(new Intent(this, Wallet.class));
                break;

            case R.id.button4://Add Coupon
                startActivity(new Intent(this, AddCoupon.class));
                break;

            case R.id.button5://Notification
                startActivity(new Intent(this, Notification.class));
                break;

            case R.id.button6://Help
                startActivity(new Intent(this, Help.class));
                break;

            case R.id.button7://AboutUs
                startActivity(new Intent(this, AboutUs.class));
                break;

            case R.id.button8://Logout
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                break;

            case R.id.showEscooters:
                shouldMapMove = false;
                // ADDING LOCATIONS
                for (int i = 1; i < 7; i++) {
                    Location location = createLocation(myLocation, i, 300);
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .snippet("" + i)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_escooter)));
                }
                showEscooters.setVisibility(View.GONE);
                Toast.makeText(HomePage.this, "Select escooter you want to book!", Toast.LENGTH_LONG).show();
                break;
        }


    }
}
