package dev.sirdir;

import com.opencsv.CSVWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;

public class BankruptcyToCSV {

    final String appendPagination = "&page=";
    final String baseUrl = "https://www.nwaonline.com/";

    public static void main(String[] args) throws IOException {
        BankruptcyToCSV parsing = new BankruptcyToCSV();
        //no point in using more than 10 pagination because after data visible only with subscription
        int iterations = (args.length == 1 || args.length >= 10)
                ? Integer.parseInt(args[0])
                : 10;
        parsing.doStuff(iterations);
    }

    private void doStuff(int iterations) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(System.nanoTime() + "test.csv"))) {
            csvWriter.writeNext(new String[]{"date","all","c7","c13"});
            for (int i = 1; i <= iterations; i++) {
                String searchRelativeUrl = "/search/?query=bankruptcy+watch";
                String initialUrl = createUrl(baseUrl, searchRelativeUrl, appendPagination, String.valueOf(i));
//        RestAssured
//            String pageHtml = get(initialUrl).asString();
                Document doc = Jsoup.connect(initialUrl).get();
                Elements links = doc.select(".recommended__article-title > a");
                for (Element link : links) {
                    //skip if article not with stats
                    if (!link.text().equalsIgnoreCase("bankruptcy watch")) continue;

                    Attributes attr = link.attributes();
                    String href = attr.get("href");
                    String articleUrl = createUrl(baseUrl, href);
                    Document doc2 = Jsoup.connect(articleUrl).get();
                    String date = doc2.selectFirst(".most-recent-article-bi-line").text();
                    Elements bankruptRows = doc2.select("[itemprop='articleBody'] > p");
                    int all = bankruptRows.size();
                    long c7 = bankruptRows.parallelStream().filter(el -> el.text().contains("Chapter 7")).count();
                    long c13 = bankruptRows.parallelStream().filter(el -> el.text().contains("Chapter 13")).count();
                    csvWriter.writeNext(new String[]{date, String.valueOf(all), String.valueOf(c7), String.valueOf(c13)});
                }
            }
        }
    }

    private String createUrl(String... partsOfUrl) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : partsOfUrl) {
            stringBuilder.append(part);
        }
        return stringBuilder.toString();
    }
}
