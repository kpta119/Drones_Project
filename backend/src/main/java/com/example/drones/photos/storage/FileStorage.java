package com.example.drones.photos.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileStorage {
    String uploadFile(MultipartFile file, String userSubdirectory) throws IOException;

    void deleteFile(String key);

    default List<String> uploadFiles(List<MultipartFile> files, String userSubdirectory) throws IOException {
        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file, userSubdirectory));
        }
        return urls;
    }
}
