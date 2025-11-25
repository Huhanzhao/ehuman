package com.helper.services.controller;

import com.helper.services.service.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/aaaaa")
public class TestController {
    @Autowired
    private Upload uploadServices;


    @PutMapping(value = "/helsssslo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String hello(@RequestParam("file") MultipartFile file) {
        String s = uploadServices.uploadResourceFile(file);
        return s;
    }
}
