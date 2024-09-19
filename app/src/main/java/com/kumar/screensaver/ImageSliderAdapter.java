package com.kumar.screensaver;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
    private Context context;
    private List<Integer> imageList;
    // The package name of the single app to open
    private final String externalAppPackage = "com.ingenico.template";


    // Add a reference to the external app package names (assuming you have 3 images and 3 apps)
//    private final List<String> externalAppPackages = Arrays.asList(
//            "com.example.foodorderingapplication"
    //            "com.example.foodorderingapplication2"

//    );

    public ImageSliderAdapter(Context context, List<Integer> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_slider_item, parent, false);
        return new ImageViewHolder(view);
    }


    // Method to open the single external app
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.imageView.setImageResource(imageList.get(position));
        holder.imageView.setImageResource(imageList.get(position));

        // Set click listener for each image
        holder.imageView.setOnClickListener(v -> openExternalApp(context, externalAppPackage));
    }
    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
    // Method to open the single external app
    private void openExternalApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            // App not found, show a message to the user
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show();
        }
    }
}
