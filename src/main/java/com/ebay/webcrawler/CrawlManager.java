package com.ebay.webcrawler;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class CrawlManager {

    /** Amount of threads running on single webpage */
    private static final int THREAD_COUNT = 10;
    /** Already downloaded urls */
    private final ConcurrentHashMap<URL, Boolean> masterUrlsMap = new ConcurrentHashMap<>();
    /** List of FetchPage threads*/
    private final List<Future<CrawlPage>> futures = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    /** Url of the main web page*/
    private final String urlBase;
    /** Maximum urls to download from the web*/
    private final int maxUrls;

    public CrawlManager(String url, int maxUrls) {
        this.maxUrls = maxUrls;
        this.urlBase = url.replaceAll("(.*//.*/).*", "$1");
    }

    public void startCrawling() {
        try {
            submitNewURL(new URL(urlBase));
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
        //till required pages observed or downloaded all pages
        while (checkPageFetch())
            ;
        executorService.shutdown();
    }

    /**
     * Creates the CrawlPage object for a given url and drops it on the execution queue.
     * After creating the task, puts the Future object into a list for further monitoring.
     *
     * @param url Url of the web page
     */
    private void submitNewURL(URL url) {
        CrawlPage crawlPage = new CrawlPage(url);
        Future<CrawlPage> future = executorService.submit(crawlPage);
        futures.add(future);
        //        executorService.shutdown();
    }


    /**
     * Checks the status of all the  running  threads on the web page and collects links.
     *
     * @return false = all the threads are done
     */
    private boolean checkPageFetch() {
        Set<CrawlPage> pageSet = new HashSet<>();
        Iterator<Future<CrawlPage>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<CrawlPage> future = iterator.next();
            if (future.isDone()) {
                iterator.remove();
                try {
                    pageSet.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        for (CrawlPage crawlPage : pageSet) {
            addFromCrawlPage(crawlPage);
        }
        return (futures.size() > 0);
    }

    /**
     * Gets the URLs from the crawl page, saves the url into the "to-do" list.
     *
     * @param crawlPage object containing the URL list
     */
    private void addFromCrawlPage(CrawlPage crawlPage) {
        for (URL url : crawlPage.getUrlSet()) {
            submitNewURL(url);
        }
    }

    /**
     * Prevents visiting the same page twice and visiting more than required links.
     *
     * @param url Potentially URL for adding
     * @return true- if url isn't added yet and didn't reached maximum links
     */
    private boolean shouldVisit(URL url) {
        if (masterUrlsMap.containsKey(url)) {
            return false;
        }
        return masterUrlsMap.size() < maxUrls;
    }

    /**
     * This object that observes specific web page.
     */
    private class CrawlPage implements Callable<CrawlPage> {

        private final int TIMEOUT = 5000;
        private final URL url;
        /**
         * Set of the links in the current web page
         */
        private final Set<URL> urlSet = new HashSet<>();

        public CrawlPage(URL url) {
            this.url = url;
        }

        @Override
        public CrawlPage call() throws Exception {
            Document document = Jsoup.parse(url, TIMEOUT);
            downloadPageAndExtractUrls(document.select("a[href]"));
            return this;
        }

        /**
         * @param links Link to download and extract links from this page.
         */
        private void downloadPageAndExtractUrls(Elements links) {
            for (Element link : links) {
                String href = link.attr("href");
                if (StringUtils.isBlank(href) || href.startsWith("#")) {
                    continue;
                }
                try {
                    URL nextUrl = new URL(url, href);
                    if (!masterUrlsMap.containsKey(nextUrl)) {
                        if (shouldVisit(nextUrl)) {
                            downloadWebPage(nextUrl);
                            urlSet.add(nextUrl);
                        }
                    }
                } catch (MalformedURLException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        /**
         * Downloads the current web page.
         * If failed - trying 3 times with 5 seconds between retrying.
         *
         * @param url URL to download
         */
        public void downloadWebPage(URL url) {
            int retries = 3;
            boolean succeed = false;
            while (retries > 0 && !succeed) {
                try {
                    BufferedReader readr =
                            new BufferedReader(new InputStreamReader(url.openStream()));
                    System.out.println("Downloading page from url ... " + url.toString());
                    String fileName = url.toString().replaceAll("[^a-zA-Z0-9\\.\\-]", "");
                    FileWriter fileWriter = new FileWriter(fileName + ".html");
                    BufferedWriter writer = new BufferedWriter(fileWriter);
                    // read each line from stream till end
                    String line;
                    while ((line = readr.readLine()) != null) {
                        writer.write(line);
                    }
                    readr.close();
                    writer.close();
                    System.out.println("Successfully downloaded: " + url.toString());
                    masterUrlsMap.put(url, true);
                } catch (IOException e) {
                    retries = getRetries(url, retries);
                    continue;
                }
                succeed = true;
            }
        }

        /**
         * Prints error and decreased retrying attempts.
         *
         * @param url     URL failed to download
         * @param retries amount of retrying
         * @return amount of remains retrying
         */
        private int getRetries(URL url, int retries) {
            System.err.println("Failed download from: " + url.toString() + ".");
            retries--;
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
            return retries;
        }

        public Set<URL> getUrlSet() {
            return urlSet;
        }
    }

}
