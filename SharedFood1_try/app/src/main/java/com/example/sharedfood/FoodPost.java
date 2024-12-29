package com.example.sharedfood;

public class FoodPost {
    private String foodDescription;
    private boolean isKosher;
    private boolean isHot;
    private boolean isCold;
    private boolean isClosed;
    private boolean isDairy;
    private boolean isMeat;
    private String imageUrl;  // URL of the image uploaded (if available)

    // Constructor to initialize the food post with description and properties
    public FoodPost(String foodDescription, boolean isKosher, boolean isHot, boolean isCold, boolean isClosed, boolean isDairy, boolean isMeat, String imageUrl) {
        this.foodDescription = foodDescription;
        this.isKosher = isKosher;
        this.isHot = isHot;
        this.isCold = isCold;
        this.isClosed = isClosed;
        this.isDairy = isDairy;
        this.isMeat = isMeat;
        this.imageUrl = imageUrl;  // Set image URL
    }

    // Getter and setter methods for all the properties
    public String getFoodDescription() {
        return foodDescription;
    }

    public boolean isKosher() {
        return isKosher;
    }

    public boolean isHot() {
        return isHot;
    }

    public boolean isCold() {
        return isCold;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isDairy() {
        return isDairy;
    }

    public boolean isMeat() {
        return isMeat;
    }
    public String getImageUrl() {
        return imageUrl;
    }
}
