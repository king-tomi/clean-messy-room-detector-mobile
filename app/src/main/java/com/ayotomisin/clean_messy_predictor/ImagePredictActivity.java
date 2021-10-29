package com.ayotomisin.clean_messy_predictor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ayotomisin.clean_messy_predictor.databinding.ActivityImagePredictBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Matrix;

import org.tensorflow.lite.Interpreter;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
public class ImagePredictActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityImagePredictBinding binding;
    private ImageView input;
    private ActivityResultLauncher<Intent> launchCaptureActivity;
    private ActivityResultLauncher<Intent> launchCameraActivity;
    private String currentPhotoFilePath;
    private ByteBuffer model;
    Matrix feature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImagePredictBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_image_predict);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        launchCaptureActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri selectedImage = data.getData();
                                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                                if (selectedImage != null) {
                                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

                                    if(cursor !=  null) {
                                        cursor.moveToFirst();

                                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                                        String picturePath = cursor.getString(columnIndex);

                                        input.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                    }

                                    assert cursor != null;
                                    cursor.close();
                                }
                            }
                        }
                    }
                }
        );

        launchCameraActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Bundle extras = data.getExtras();
                                Bitmap im = (Bitmap) extras.get("data");
                                input.setImageBitmap(im);
//                                Uri selectedImage = data.getData();
//                                File file = new File(selectedImage.getPath());
//                                try {
//                                    InputStream ims = new FileInputStream(file);
//                                    input.setImageBitmap(BitmapFactory.decodeStream(ims));
//                                } catch (FileNotFoundException e) {
//                                    return;
//                                }
                            }
                        }
                    }
                }
        );

        makePredictionAndDisplay();
    }

    private void makePredictionAndDisplay() {
        input = findViewById(R.id.input_image);
        input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(ImagePredictActivity.this);
            }
        });

        feature = input.getImageMatrix();
        feature.setScale(299, 299);
        String result = predict(feature);
        TextView solution = findViewById(R.id.solution);

        Button button = findViewById(R.id.predict);
        //if button is clicked, it should make a prediction
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solution.setText(result);
            }
        });
    }

    private void selectImage(Context context) {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Image to make detection");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            private Uri photoUri;

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (options[which].equals("Take Photo")) {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    takePicture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    takePicture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    File f = null;
                    try {
                        f = createImageFile();
                    } catch (IOException ex) {
                        return;
                    }
                    if (f != null) {
                        photoUri = FileProvider.getUriForFile(ImagePredictActivity.this,
                                BuildConfig.APPLICATION_ID + ".provider",
                                f);

                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                        launchCameraActivity.launch(takePicture);
                    }

                }
                else if (options[which].equals("Choose from Gallery")) {
                    Intent capture = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    launchCaptureActivity.launch(capture);
                }
                else if (options[which].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });

        builder.show();
    }

    private String predict (Matrix image) {
        String message ="";
        try {
            model = loadModel(this.getAssets(), "clean_messy_mobile.tflite");
            Interpreter tflite = new Interpreter(model);
            ByteBuffer output = ByteBuffer.allocateDirect(4);
            output.order(ByteOrder.nativeOrder());
            tflite.run(image, output);

            output.rewind();
            float f = output.getFloat();

            if (f < 0.5) {
               message = "This is a messy room";
            }
            else {
                message = "This is a clean room";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

    private ByteBuffer loadModel(AssetManager asset, String path) throws IOException {
        AssetFileDescriptor fileDescriptor = asset.openFd(path);
        FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel channel = fileInputStream.getChannel();
        long offset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return channel.map(FileChannel.MapMode.READ_ONLY, offset, declaredLength);
    }

    private File createImageFile() throws IOException {
        String imageName = "room_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Camera"
        );
        File image = File.createTempFile(
                imageName,
                ".jpg",
                storageDir
        );

        currentPhotoFilePath = "file:" + image.getAbsolutePath();
        return image;
    }
}