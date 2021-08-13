package com.ayotomisin.clean_messy_predictor;

import android.graphics.Matrix;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ayotomisin.clean_messy_predictor.databinding.ActivityImagePredictBinding;

public class ImagePredictActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityImagePredictBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImagePredictBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_image_predict);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        makePredictionAndDisplay();
    }

    private void makePredictionAndDisplay() {
        ImageView input = findViewById(R.id.input_image);
        Matrix feature = input.getImageMatrix();
        String result = Predictor.predict(feature);
        TextView show = findViewById(R.id.solution);

        Button button = findViewById(R.id.predict);
        //if button is clicked, it should make a prediction
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String response = "";
                if (result.equals("0")) {
                    response = "This is not a messy room";
                }
                else { response = "This is a messy room, please clean"; }
                show.setText(response);
            }
        });
    }
}