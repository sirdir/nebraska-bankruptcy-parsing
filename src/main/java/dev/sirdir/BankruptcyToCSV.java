package dev.sirdir;

import com.opencsv.CSVWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

public class BankruptcyToCSV {

    final String appendPagination = "&page=";
    final String baseUrl = "https://www.nwaonline.com/";
    final DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

    public static void main(String[] args) throws IOException {
        BankruptcyToCSV parsing = new BankruptcyToCSV();
        //no point in using more than 10 pagination because after data visible only with subscription
        int iterations = (args.length == 1 && Integer.parseInt(args[0]) >= 10)
                ? Integer.parseInt(args[0])
                : 10;
        parsing.runParser(iterations);
    }

    private void runParser(int iterations) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(System.nanoTime() + "test.csv"))) {
            //headers
            String[] row = {"date", "all", "c7", "c13"};
            csvWriter.writeNext(row);
            for (int i = 1; i <= iterations; i++) {
                String searchRelativeUrl = "/search/?query=bankruptcy+watch";
                String initialUrl = createUrl(baseUrl, searchRelativeUrl, appendPagination, String.valueOf(i));

                Document doc = Jsoup.connect(initialUrl).get();
                Elements allArticles = doc.select(".recommended__article-title > a");

                //filter wrong articles
                List<Element> filteredArticles = allArticles.parallelStream()
                        .filter(el -> el.text().equalsIgnoreCase("bankruptcy watch"))
                        .collect(Collectors.toList());
                parseArticle(csvWriter, filteredArticles, row);
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

    private void parseArticle(CSVWriter csvWriter, List<Element> links, String[] row) throws IOException {
        for (Element link : links) {
            Attributes attr = link.attributes();
            String href = attr.get("href");
            String articleUrl = createUrl(baseUrl, href);
            Document article = Jsoup.connect(articleUrl).get();
            String dateStr = article.selectFirst(".most-recent-article-bi-line").text();
            //just cant parse full string because of "a.m." or "p.m." have dots
            //March 22, 2020 at 1:52 a.m.
            LocalDate date = LocalDate.parse(dateStr.split(" at")[0], dtf);
            Elements bankruptRows = article.select("[itemprop='articleBody'] > p");
            int all = bankruptRows.size();
            long c7 = bankruptRows.parallelStream().filter(el -> el.text().contains("Chapter 7")).count();
            long c13 = bankruptRows.parallelStream().filter(el -> el.text().contains("Chapter 13")).count();
            row[0] = date.toString();
            row[1] = String.valueOf(all);
            row[2] = String.valueOf(c7);
            row[3] = String.valueOf(c13);
            csvWriter.writeNext(row);
        }
    }
}
