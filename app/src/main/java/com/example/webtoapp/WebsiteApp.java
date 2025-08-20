package com.example.webtoapp;

import android.graphics.Bitmap;

public class WebsiteApp {
    private String name;
    private String url;
    private Bitmap icon;

    public WebsiteApp(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public WebsiteApp(String name, String url, Bitmap icon) {
        this.name = name;
        this.url = url;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }
}
