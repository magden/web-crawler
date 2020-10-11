# web-crawler
Web crawler application that downloads content of
websites (one file per web page).
Works as follows: Once a web page is downloaded scrapes the content to
find urls pointing to other pages of the website.
I've created 5 threads to read the csv file, each one of them may run 2 other threads that  executes downloading content from the web site (Max concurrent connections per website: 2 and maximum total connections: 10). 
