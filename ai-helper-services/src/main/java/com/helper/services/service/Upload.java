package com.helper.services.service;

import org.springframework.web.multipart.MultipartFile;

public interface Upload {
    String uploadResourceFile(MultipartFile file);
}
