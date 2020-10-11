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

    public static void main(String[] args) throws IOException {

        SpringApplication.run(WebCrawlerApplication.class, args);
        if (args.length >= 1) {
            System.out.println("Starting ");
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            System.out.println("Starting ");
            String csvFilePath = args[0];
            BufferedReader csvBufferedReader = Files.newBufferedReader(Paths.get(csvFilePath));
            String row;
            while ((row = csvBufferedReader.readLine()) != null) {
                //"clean" = whitespaces less
                String cleanRow = row.replaceAll("\\s+", "");
                String[] data = cleanRow.split(",");
                if (data.length == 2) {
                    Crawler crawler = new Crawler(data[0], Integer.parseInt(data[1]));
                    executorService.submit(crawler);
                } else {
                    System.out.println("Wrong row format.");
                }
            }
            executorService.shutdown();
        } else {
            System.out.println("Please insert csv file as a parameter. args size " + args.length);
        }

    }

}
