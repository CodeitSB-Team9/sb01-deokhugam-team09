package com.codeit.sb01_deokhugam.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfig {

    public static final String TESSDATA_PREFIX = "TESSDATA_PREFIX";

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        String tessdataPath = System.getenv(TESSDATA_PREFIX);

        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("kor+eng");
        return tesseract;
    }
}
