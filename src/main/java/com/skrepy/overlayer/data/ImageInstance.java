package com.skrepy.overlayer.data;

public class ImageInstance {
    private int id;
    private String path;

    public ImageInstance() {
    }

    public ImageInstance(int id, String path) {
        this.id = id;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
