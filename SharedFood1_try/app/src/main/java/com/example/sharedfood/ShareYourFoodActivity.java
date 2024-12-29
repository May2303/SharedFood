package com.example.sharedfood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShareYourFoodActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 102;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;

    private EditText foodDescriptionEditText;
    private Button shareFoodButton, selectImageButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri imageUri;
    private ImageView imageView;
    private Uri photoURI;

    // Food properties
    private boolean isKosher, isHot, isCold, isClosed, isDairy, isMeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_your_food);

        // Initialize views
        initializeViews();

        // Initialize Firebase services
        initializeFirebase();

        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        foodDescriptionEditText = findViewById(R.id.foodDescriptionEditText);
        shareFoodButton = findViewById(R.id.shareFoodButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        imageView = findViewById(R.id.imageView);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    private void setupButtonListeners() {
        // Select Image button listener
        selectImageButton.setOnClickListener(v -> showImageSourceDialog());

        // Share Food button listener
        shareFoodButton.setOnClickListener(v -> {
            String foodDescription = foodDescriptionEditText.getText().toString().trim();

            if (!foodDescription.isEmpty()) {
                // Create food post object
                FoodPost foodPost = new FoodPost(
                        foodDescription,
                        isKosher, isHot, isCold,
                        isClosed, isDairy, isMeat,
                        null
                );

                // Upload image if exists
                if (imageUri != null) {
                    uploadImageToFirebase(foodPost);
                } else {
                    saveFoodPost(foodPost, null);
                }
            } else {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(new CharSequence[] {"Camera", "Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkGalleryPermission();
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    GALLERY_PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.sharedfood.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Gallery permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                imageUri = photoURI;
                imageView.setImageURI(imageUri);
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                imageUri = data.getData();
                imageView.setImageURI(imageUri);
            }
        }
    }

    private void uploadImageToFirebase(FoodPost foodPost) {
        if (imageUri != null) {
            StorageReference imageRef = storageReference.child("foodImages/" +
                    System.currentTimeMillis() + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveFoodPost(foodPost, imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveFoodPost(foodPost, null);
        }
    }

    private void saveFoodPost(FoodPost foodPost, String imageUrl) {
        if (imageUrl != null) {
            foodPost = new FoodPost(
                    foodPost.getFoodDescription(),
                    foodPost.isKosher(), foodPost.isHot(),
                    foodPost.isCold(), foodPost.isClosed(),
                    foodPost.isDairy(), foodPost.isMeat(),
                    imageUrl
            );
        }

        db.collection("foodPosts")
                .add(foodPost)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Food shared successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to share food", Toast.LENGTH_SHORT).show();
                });
    }
}