package com.helper.knowledge.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ReadFilesUtil {
    public static String extractText(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".txt")) {
            // MultipartFile 转 byte[] -> String
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else if (fileName.endsWith(".pdf")) {
            // MultipartFile 转 InputStream
            try (InputStream is = file.getInputStream();
                 RandomAccessRead rar = new RandomAccessReadBuffer(is);
                 PDDocument document = Loader.loadPDF(rar)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if (fileName.endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    sb.append(p.getText()).append("\n");
                }
                return sb.toString();
            }
        } else if (fileName.endsWith(".doc")){
            try (InputStream is = file.getInputStream();
                 HWPFDocument doc = new HWPFDocument(is)) {
                WordExtractor extractor = new WordExtractor(doc);
                return extractor.getText();
            }
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + fileName);
        }
    }
}
