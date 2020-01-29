package com.rotciv.travelpoints.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.rotciv.travelpoints.R;
import com.rotciv.travelpoints.helper.Permissions;

public class MainActivity extends IntroActivity {

    private String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        addSlide(new SimpleSlide.Builder()
                .title(R.string.welcome)
                .description(R.string.description_1)
                .background(android.R.color.white)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.how_to_use)
                .description(R.string.description_2)
                .background(android.R.color.white)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.how_to_use)
                .description(R.string.description_3)
                .background(android.R.color.white)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.how_to_use)
                .description(R.string.description_4)
                .background(android.R.color.white)
                .build());

        addSlide(new FragmentSlide.Builder()
                .background(android.R.color.white)
                .fragment(R.layout.intro_fragment)
                .canGoForward(false)
                .build()
        );
    }

    public void accessApp (View view) {

        if (Build.VERSION.SDK_INT >= 23 ) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // manda pra activity
            } else {
                Permissions.validatePermissions(permissions, this, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for ( int permissionResult : grantResults ) {
            if ( permissionResult == PackageManager.PERMISSION_DENIED) {
                alertPermissionValidation();
            }
        }
    }

    public void alertPermissionValidation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle( R.string.permission_denied );
        builder.setMessage(R.string.permission_denied_message);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
