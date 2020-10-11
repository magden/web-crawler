package com.ebay.webcrawler;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Object that scans url file, for each row( web site) creates new thread for monitoring.
 */
public class CrawlerStarter {

    private final String[] args;
    private static final int AMOUNT_OF_RUNNERS = 5;

    public CrawlerStarter(String... args) {
        this.args = args;
    }

    public void start() {
        try {
            if (args.length >= 1) {
                ExecutorService executorService = Executors.newFixedThreadPool(AMOUNT_OF_RUNNERS);
                String csvFilePath = args[0];
                BufferedReader csvBufferedReader = Files.newBufferedReader(Paths.get(csvFilePath));
                String row;
                //each row from the file creates new crawler and runs it
                while ((row = csvBufferedReader.readLine()) != null) {
                    //"clean" = whitespaces less
                    String cleanRow = row.replaceAll("\\s+", "");
                    String[] data = cleanRow.split(",");
                    if (data.length == 2) {
                        try {
                            Crawler crawler = new Crawler(data[0], Integer.parseInt(data[1]));
                            executorService.submit(crawler);
                        } catch (NumberFormatException e) {
                            System.out.println("Failed convert :" + data[1] + " to number");
                        }
                    } else {
                        System.out.println("Wrong row format.");
                    }
                }
                executorService.shutdown();
            } else {
                System.out.println("Please insert csv file as a parameter. args size " + args
                        .length);
            }
        } catch (IOException e) {
            System.err.println("Failed to read csv file.");
        }
    }
}

