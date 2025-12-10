package com.example.drones.photos.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("test")
public class InMemoryFileStorage implements FileStorage {

    private final Map<String, byte[]> storage = new HashMap<>();

    @Override
    public String uploadFile(MultipartFile file, String userSubdirectory) {
        String fakeUrl = "https://fakeStorage.com/" + userSubdirectory + "/" + file.getOriginalFilename();
        try {
            storage.put(fakeUrl, file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fakeUrl;
    }
}
