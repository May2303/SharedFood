package com.example.sharedfood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ShareYourFoodActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 102;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;

    private EditText foodDescriptionEditText;
    private Button uploadPostButton , selectImageButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
   private Uri imageUri;
    private ImageView imageView;

    List<String> selectedFilters = new ArrayList<>();
    Post post;
    String imageUrl;

    CheckBox kosherCheckBox, hotCheckBox, coldCheckBox, closedCheckBox, dairyCheckBox, meatCheckBox;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (imageUri != null) {
                        if (post == null) post = new Post();
                        post.setImageUri(imageUri);
                        imageView.setImageURI(imageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (post == null) post = new Post();
                    // אם יש לך URI מוגדר מראש למצלמה
                    if (imageUri != null) {
                        post.setImageUri(imageUri);
                        imageView.setImageURI(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_your_food);

        kosherCheckBox = findViewById(R.id.kosherCheckBox);
        hotCheckBox = findViewById(R.id.hotCheckBox);
        coldCheckBox = findViewById(R.id.coldCheckBox);
        closedCheckBox = findViewById(R.id.closedCheckBox);
        dairyCheckBox = findViewById(R.id.dairyCheckBox);
        meatCheckBox = findViewById(R.id.meatCheckBox);

        // Initialize views
        foodDescriptionEditText = findViewById(R.id.foodDescriptionEditText);
        uploadPostButton  = findViewById(R.id.uploadPostButton );
        selectImageButton = findViewById(R.id.selectImageButton);
        imageView = findViewById(R.id.imageView);

        // Initialize Firebase services
        //FirebaseApp.initializeApp(ShareYourFoodActivity.this);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Set up button click listeners
        selectImageButton.setOnClickListener(v -> showImageSourceDialog());

       /*
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        });
        */

        // Share Food button listener
        uploadPostButton.setOnClickListener(v -> {
            String foodDescription = foodDescriptionEditText.getText().toString().trim();

            if (foodDescription.isEmpty()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
                return;
            }
            else{
                post.setDescription(foodDescription);
                post.setImageUri(imageUri);
                updateSelectedFilters();
            }
         uploadPost();
        });
    }

    private void updateSelectedFilters() {
        if(post.getFilters()!=null) {

            post.getFilters().clear();  // מנקה את הרשימה קודם כל
        }
        if (meatCheckBox.isChecked()) selectedFilters.add("Meat");
        if (dairyCheckBox.isChecked()) selectedFilters.add("Dairy");
        if (hotCheckBox.isChecked()) selectedFilters.add("Hot");
        if (coldCheckBox.isChecked()) selectedFilters.add("Cold");
        if (kosherCheckBox.isChecked()) selectedFilters.add("Kosher");
        post.setFilters(selectedFilters);
    }

    private boolean isValidImageUri(Uri uri) {
        if (uri == null) return false;
        try {
            getContentResolver().openInputStream(uri).close();
            return true;
        } catch (Exception e) {
            return false;
        }
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
                imageUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                try {
                    cameraLauncher.launch(takePictureIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
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
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null && data.getData() != null) {
                post.setImageUri(data.getData());
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null && data.getData() != null) {
                post.setImageUri(data.getData());
            }
            if (post.getImageUri() != null) {
                try {
                    imageView.setImageURI(post.getImageUri());
                } catch (Exception e) {
                }
            }
        }
    }
 */

    private void uploadPost() {
        if (post == null || post.getImageUri() == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        storageRef = FirebaseStorage.getInstance().getReference().child("foodImages/" + UUID.randomUUID().toString() + ".jpg");
        storageRef.putFile(post.getImageUri()).addOnSuccessListener(taskSnapshot -> {
            Log.d("Firebase", "Upload successful");
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d("Firebase", "Download URL received: " + uri);
                post.setImageUrl(uri.toString());
                savePostToFirestore();
            }).addOnFailureListener(e -> {
                Log.e("Firebase", "Failed to get download URL", e);
            });
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Image upload failed", e);
        });
        savePostToFirestore();
    }

    private void savePostToFirestore() {
        // הפניה ל-Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // יצירת אובייקט פוסט
        Map<String, Object> foodPost = new HashMap<>();
        foodPost.put("description", post.getDescription());
        foodPost.put("filters", post.getFilters());
        foodPost.put("imageUrl", post.getImageUrl());
        foodPost.put("timestamp", System.currentTimeMillis());

        // שמירת הפוסט בקולקציה "posts"
        firestore.collection("posts")
                .add(foodPost)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Post uploaded successfully with ID: " + documentReference.getId());
                    Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Post upload failed", e);
                    Toast.makeText(this, "Post upload failed", Toast.LENGTH_SHORT).show();
                });
    }
}
