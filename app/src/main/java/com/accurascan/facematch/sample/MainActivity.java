package com.accurascan.facematch.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.accurascan.facematch.ui.FaceMatchActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void AccuraFaceMatch(View view) {
        Intent intent = new Intent(this, FaceMatchActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
