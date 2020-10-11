package com.ebay.webcrawler;

/**
 * Responsible for a crawling a current web page.
 */
public class Crawler implements Runnable {

    private final String url;
    private final int maxLink;

    public Crawler(String url, int maxLink) {
        this.url = url;
        this.maxLink = maxLink;
    }

    @Override
    public void run() {
        CrawlManager crawlManager = new CrawlManager(url, maxLink);
        System.out.println("Starts crawling");
        crawlManager.startCrawling();
    }
}
