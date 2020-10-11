package com.ebay.webcrawler;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;


public class CrawlManager {

    /** Amount of threads running on single webpage */
    private static final int THREAD_COUNT = 2;
    /** Already downloaded urls */
    private final Map<URL, Boolean> masterUrlsMap = new ConcurrentHashMap<>();
    /** List of FetchPage threads*/
    private final Set<Future<CrawlerPage>> futures = new HashSet<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    /** Url of the main web page*/
    private final String urlBase;
    /** Maximum urls to download from the web*/
    private final int maxUrls;


    public CrawlManager(String url, int maxUrls) {
        this.maxUrls = maxUrls;
        this.urlBase = url.replaceAll("(.*//.*/).*", "$1");
    }

    public void startCrawlingWebSite() {
        try {
            submitNewURL(new URL(urlBase));
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
        //till required pages observed or downloaded all pages
        while (crawlersStillCollectingUrls())
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
        CrawlerPage crawlerPage = new CrawlerPage(url);
        Future<CrawlerPage> future = executorService.submit(crawlerPage);
        futures.add(future);
    }


    /**
     * Checks the status of all the running threads on the web page and collects links.
     *
     * @return false = all the threads are done
     */
    private boolean crawlersStillCollectingUrls() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        Set<CrawlerPage> pageSet = new HashSet<>();
        Iterator<Future<CrawlerPage>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<CrawlerPage> future = iterator.next();
            if (future.isDone()) {
                iterator.remove();
                try {
                    pageSet.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        //each crawler page holds set of the urls for a further monitoring
        for (CrawlerPage crawlerPage : pageSet) {
            for (URL url : crawlerPage.getUrlSet()) {
                submitNewURL(url);
            }
        }
        return (futures.size() > 0);
    }


    /**
     * This object that observes specific web page. Downloads each link and collects them to the list for
     * further monitoring.
     */
    private class CrawlerPage implements Callable<CrawlerPage> {

        private final int TIMEOUT = 5000;
        private final URL url;
        /*** Set of the links in the current web page */
        private final Set<URL> urlSet = new HashSet<>();

        public CrawlerPage(URL url) {
            this.url = url;
        }

        @Override
        public CrawlerPage call() throws Exception {
            Document document = Jsoup.parse(url, 1000);
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
                    //if url hadn't visited yet puts to master urls map, downloads the page, and sets to the set
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
         * Downloads the current web page and puts the url to the master urls map.
         * If failed - trying 3 times with 5 seconds waiting between retrying.
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
                    String fileName = url.toString().replaceAll("[^a-zA-Z0-9.\\-]", "");
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
                    masterUrlsMap.put(url, false);
                } catch (IOException e) {
                    retries = getRetries(url, retries);
                    continue;
                }
                succeed = true;
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
         * Prints error, decreases retrying attempts, sleeps before retrying.
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
