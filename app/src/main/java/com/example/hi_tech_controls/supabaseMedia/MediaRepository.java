package com.example.hi_tech_controls.supabaseMedia;

import java.util.ArrayList;
import java.util.List;

public class MediaRepository {
    private static MediaRepository instance;
    private List<String> mediaUrls = new ArrayList<>();

    private MediaRepository() {
    }

    public static MediaRepository getInstance() {
        if (instance == null) {
            instance = new MediaRepository();
        }
        return instance;
    }

    public void addMediaUrl(String url) {
        mediaUrls.add(url);
    }

    public List<String> getMediaUrls() {
        return new ArrayList<>(mediaUrls);
    }

    public void clearMediaUrls() {
        mediaUrls.clear();
    }

    public int getMediaCount() {
        return mediaUrls.size();
    }
}