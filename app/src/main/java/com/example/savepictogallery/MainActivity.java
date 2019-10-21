package com.example.savepictogallery;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    //to represent the buttons on the GUI
    ImageButton leafButton;
    ImageButton flowerButton;
    ImageButton button;

    //to check whether or not the buttons have been pressed
    boolean leafButtonPressed = false;
    boolean flowerButtonPressed = false;

    //represent the photoFile and path name
    File photoFile = null;
    String mCurrentPhotoPath;

    //global constants
    static final int CAPTURE_IMAGE_REQUEST = 1;
    public static final int GALLERY_REQUEST = 0;
    private static final String IMAGE_DIRECTORY_NAME = "PIB";

    //Load the tensorflow inference library
    static {
        System.loadLibrary("tensorflow_inference");
    }

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/tf_model_mobilenet_sophia.pb";

    private String INPUT_NAME = "input_1";
    private String OUTPUT_NAME = "dense_2/Softmax";
    private TensorFlowInferenceInterface tf;
    private Bitmap bitmap;

    //ARRAY TO HOLD THE PREDICTIONS AND FLOAT VALUES TO HOLD THE IMAGE DATA
    float[] PREDICTIONS = new float[1000];
    private float[] floatValues;
    private int[] INPUT_SIZE = {224,224,3};

    ImageView imageView;
    TextView resultView;
    Snackbar progressBar;

    //create method which runs as the app starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializes buttons
        leafButton =  findViewById(R.id.leafButton);
        flowerButton = findViewById(R.id.flowerButton);

        //adds listener for leafButton
        leafButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //in order to control button highlighting
                if(flowerButtonPressed) {

                    flowerButton.setImageResource(R.drawable.flower);
                    flowerButtonPressed = false;
                }

                leafButtonPressed = true;
                leafButton.setImageResource(R.drawable.leafpressed);
                //Log.println(Log.ASSERT, "button", "leaf");
            }
        });

        //adds listener for flower button
        flowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //in order to control button highlighting
                if(leafButtonPressed) {

                    leafButton.setImageResource(R.drawable.leaf);
                    leafButtonPressed = false;
                }

                flowerButtonPressed = true;
                flowerButton.setImageResource(R.drawable.flowerpressed);
                //Log.println(Log.ASSERT, "button", "flower");
            }
        });

        //initializes camera button
        button = findViewById(R.id.btnCaptureImage);

        //adds listener for camera button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Log.println(Log.ASSERT, "captureImage: ", "1");

                //to make sure that the user has selected plant or leaf
                if(flowerButtonPressed || leafButtonPressed) {
                    captureImage();
                }

                //tells user to select plant or leaf
                else {

                    displayMessage(getBaseContext(), "Please select whether you would like to take a picture of a leaf or flower.");
                }
            }
        });

        //initialize tensorflow with the AssetManager and the Model
        tf = new TensorFlowInferenceInterface(getAssets(),MODEL_PATH);

        imageView = (ImageView) findViewById(R.id.imageView);
        resultView = (TextView) findViewById(R.id.results);

        progressBar = Snackbar.make(imageView,"PROCESSING IMAGE",Snackbar.LENGTH_INDEFINITE);


        final FloatingActionButton predict = (FloatingActionButton) findViewById(R.id.predict);
        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                try{

                    //READ THE IMAGE FROM ASSETS FOLDER
                    /*InputStream imageStream = getAssets().open("tulip.png");

                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                    imageView.setImageBitmap(bitmap);*/

                    progressBar.show();

                    predict(bitmap);
                }
                catch (Exception e){

                    Log.println(Log.ASSERT, "image test", "not work");
                }

            }
        });

    }

    //opens saved plants window
    public void openSavedPlants(View view) {
        Intent intent = new Intent(this, SavedPlants.class);
        startActivity(intent);
    }

    //method which opens gallery, gallery button
    public void openGallery(View view) {

        //to make sure the user has selected either leaf or flower
        if(flowerButtonPressed || leafButtonPressed) {

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST);
        }

        //tells user to select flower or leaf
        else {

            displayMessage(getBaseContext(), "Please select whether you would like to use a picture of a leaf or flower.");
        }
    }

    //method to invoke the camera
    public void captureImage()
    {
        //makes sure permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
        else
        {
            //creates the intent to access camera
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                try {

                    photoFile = createImageFile();
                    displayMessage(getBaseContext(),photoFile.getAbsolutePath());
                    //Log.println(Log.ASSERT,"photoFile path: ",photoFile.getAbsolutePath());

                    // Continue only if the File was successfully created
                    if (photoFile != null) {

                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.example.savepictogallery.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
                    }
                }

                catch (Exception ex) {

                    // Error occurred while creating the File
                    displayMessage(getBaseContext(),ex.getMessage().toString());
                }


            }

            //displays error message
            else {

                displayMessage(getBaseContext(),"Error occurred");
            }
        }
    }

    //method which is called after startActivityForResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            if(requestCode == CAPTURE_IMAGE_REQUEST) {
                bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title" , "description");
            }

            if(requestCode == GALLERY_REQUEST) {

                Uri uri = data.getData();
                try {

                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                    displayMessage(getBaseContext(), "successful");
                }

                catch(Exception e) {

                    e.printStackTrace();
                }
            }
        }
        else {

            displayMessage(getBaseContext(),"Request cancelled or something went wrong.");
        }
    }

    //method which creates the image file
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile( imageFileName,".jpg",storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //method to display messages using toast
    private void displayMessage(Context context, String message)
    {
        Toast.makeText(context,message,Toast.LENGTH_LONG).show();
    }

    //checks to see if permissions are granted
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            }
        }
    }

    //FUNCTION TO COMPUTE THE MAXIMUM PREDICTION AND ITS CONFIDENCE
    public Object[] argmax(float[] array){
        int best = -1;
        float best_confidence = 0.0f;

        for(int i = 0;i < array.length;i++){

            float value = array[i];

            if (value > best_confidence){

                best_confidence = value;
                best = i;
            }
        }

        return new Object[]{best,best_confidence};
    }

    public void predict(final Bitmap bitmap){


        //Runs inference in background thread
        new android.os.AsyncTask<Integer,Integer,Integer>(){

            @Override

            protected Integer doInBackground(Integer ...params){

                //Resize the image into 224 x 224
                Bitmap resized_image = ImageUtils.processBitmap(bitmap,224);

                //Normalize the pixels
                floatValues = ImageUtils.normalizeBitmap(resized_image,224,127.5f,1.0f);

                //Pass input into the tensorflow
                tf.feed(INPUT_NAME,floatValues,1,224,224,3);

                //compute predictions
                tf.run(new String[]{OUTPUT_NAME});

                //copy the output into the PREDICTIONS array
                tf.fetch(OUTPUT_NAME,PREDICTIONS);

                //Obtained highest prediction
                Object[] results = argmax(PREDICTIONS);


                int class_index = (Integer) results[0];
                float confidence = (Float) results[1];


                try{

                    final String conf = String.valueOf(confidence * 100).substring(0,5);

                    //Convert predicted class index into actual label name
                    final String label = ImageUtils.getLabel(getAssets().open("labels.json"),class_index);



                    //Display result on UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            progressBar.dismiss();
                            resultView.setText(label + " : " + conf + "%");

                        }
                    });

                }

                catch (Exception e){

                }

                return 0;
            }

        }.execute(0);

    }
}