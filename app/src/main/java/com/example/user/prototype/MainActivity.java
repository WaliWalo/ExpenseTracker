package com.example.user.prototype;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.max;

public class MainActivity extends AppCompatActivity {

    ImageView ivImage;
    TextView tvText;
    Button btnSave;
    Bitmap bitmap;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private static final int RC_SIGN_IN = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createSignInIntent();
        ivImage = findViewById(R.id.imageView);
        tvText = findViewById(R.id.textView);
    }

    public void save(View v){
        if(bitmap==null){
            Toast.makeText(getApplicationContext(),"No image detected.",Toast.LENGTH_LONG).show();
        }else {
            final FirebaseVisionImage fireBaseImage = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer firebaseRecognizer = FirebaseVision.getInstance().getCloudTextRecognizer();
            firebaseRecognizer.processImage(fireBaseImage)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                            process_image(firebaseVisionText);
                        }
                    });
        }
    }
    List<Float> prices = new ArrayList<>();
    Date date = new Date();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private void process_image(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        if(blocks.size()==0){
            Toast.makeText(getApplicationContext(),"No text detected.",Toast.LENGTH_LONG).show();
        }else{
            String text="";
            for(FirebaseVisionText.TextBlock block:firebaseVisionText.getTextBlocks()){
                text = text + block.getText();

            }
            String[] split = text.split("\\s+");
            //get all digits
            for(int i = 0; i < split.length; i++){
                prices.clear();
                if(split[i].matches("\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})")){
                        prices.add(Float.parseFloat(split[i]));
                }
                if(split[i].matches("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}")){
                    DateFormat format = new SimpleDateFormat("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}", Locale.ENGLISH);
                    try {
                        date = format.parse(split[i]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            float total = 0;
            int x = 0;
            //removes repeating max price
            for(int i = 0; i < prices.size(); i++){
                if(prices.get(i)==max(prices)){
                    x++;
                }
                if(x>1){
                    prices.remove(i);
                }
            }
            Float max = Collections.max(prices);
            //get Total
            for(int i = 0; i < prices.size(); i++){
                total += prices.get(i);
            }
            //get second largest for cash payments
            float secLargest = prices.get(0);
            for(int i = 1; i < prices.size(); i++){
                if(prices.get(i)>max){
                    secLargest = max;
                    max = prices.get(i);
                }
                if(prices.get(i)>secLargest && prices.get(i)!=max){
                    secLargest = prices.get(i);
                }
            }
            //checks if its credit card payment
            if(max!=(total-max)){
                total = secLargest;
            }
            tvText.setText(Float.toString(total) + date);
            //add details to firebase
            if (user != null) {
                String uid = user.getUid();
                writeNewUser(uid, total, text, date);
            } else {
                createSignInIntent();
            }

            myRef.child("Total").push().setValue(total);
        }
    }

    //write new details to database
    private void writeNewUser(String userId, Float total, String desc, Date date) {
        Details detail = new Details(userId, total, desc, date);

        myRef.child("details").child(userId).setValue(detail);
    }

    public void pickImage(View v){
        Intent intent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //from file
        if(requestCode==1 && resultCode==RESULT_OK){
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                ivImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //from camera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File file = new File(mCurrentPhotoPath);
            try {
                bitmap = MediaStore.Images.Media
                        .getBitmap(this.getContentResolver(), Uri.fromFile(file));
                ivImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //login action
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // ...
            } else {
                createSignInIntent();
            }
        }

    }

    static final int REQUEST_IMAGE_CAPTURE = 2;
    static final int WRITE_PERMISSION = 3;
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public void cameraOnClick(View view) {
        dispatchTakePictureIntent();
    }
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //menu bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.history:
                Intent i = new Intent(getApplicationContext(),History.class);
                startActivity(i);
                return true;
            case R.id.logout:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]
    }


    public void signOut() {
        // [START auth_fui_signout]
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        createSignInIntent();
                    }
                });
        // [END auth_fui_signout]
    }
}
