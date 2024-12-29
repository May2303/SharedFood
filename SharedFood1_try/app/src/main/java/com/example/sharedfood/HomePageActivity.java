package com.example.sharedfood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Navigate to ShareYourFoodActivity when the Share Food button is clicked
        findViewById(R.id.shareFoodButton).setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, ShareYourFoodActivity.class);
            startActivity(intent);
        });

        //Navigate to ContactUsActivity when the Contact Us button is clicked
        findViewById(R.id.contactUsButton).setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, activity_contact_us.class);
            startActivity(intent);
        });

        // כפתור Personal Area
        findViewById(R.id.personalAreaButton).setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, PersonalAreaActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.feedButton).setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, FeedActivity.class);
            startActivity(intent);
        });

    }
}
