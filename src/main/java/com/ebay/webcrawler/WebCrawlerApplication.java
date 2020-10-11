package com.ebay.webcrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class WebCrawlerApplication {

    public static void main(String[] args) {
        //        SpringApplication.run(WebCrawlerApplication.class, args);
        CrawlerStarter starter = new CrawlerStarter(args);
        starter.start();
    }


}
