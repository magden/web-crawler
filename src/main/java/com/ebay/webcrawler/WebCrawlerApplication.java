package com.ebay.webcrawler;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebCrawlerApplication {

    public static void main(String[] args) {
        CrawlerStarter starter = new CrawlerStarter(args);
        starter.start();
    }
}
