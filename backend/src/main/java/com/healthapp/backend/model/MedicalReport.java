package com.healthapp.backend.model;

public class MedicalReport {
    private String id;
    private String filename;
    private String uploadDate;
    private String size;
    private String url;

    public MedicalReport() {}

    public MedicalReport(String id, String filename, String uploadDate, String size, String url) {
        this.id = id;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.size = size;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
