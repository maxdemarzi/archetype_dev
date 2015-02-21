package pe.archety;

import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.mediawiki.actions.queries.BacklinkTitles;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.commons.validator.routines.UrlValidator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;


public class Page {

    public static boolean isValidURL(String text) {
        return UrlValidator.getInstance().isValid(text);
    }

    public static URL getValidURL(String text) {
        try {
            return new URL(text);
        } catch (Throwable t) {
            throw Exception.invalidURL;
        }
    }

    public static boolean isValidWikipediaURL(URL url) {
        return url.getHost().equals("en.wikipedia.org");
    }

    public static boolean isWikipediaURLFound(URL url) throws IOException {
        //TODO: Maybe Cache or use a bloom filter?
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("HEAD");
        return (huc.getResponseCode() == 200);
    }

    public static String getPageURL(String text) throws IOException {
        if(text.isEmpty()) {
            throw Exception.missingQueryParameters;
        }

        if (isValidURL(text)) {
            URL url = getValidURL(text);
            if (isValidWikipediaURL(url)) {
                if(isWikipediaURLFound(url)) {
                    return text;
                } else {
                    throw Exception.wikipediaURLNotFound;
                }
            } else {
                throw Exception.invalidWikipediaURL;
            }
        } else {
            throw Exception.invalidURL;
        }
    }

    public static Node getPageNode(String url, GraphDatabaseService db) {
        Node page = IteratorUtil.singleOrNull(
                db.findNodesByLabelAndProperty(Labels.Page, "url", url));
        if(page != null) {
            return page;
        } else {
            throw Exception.pageNotFound;
        }
    }

    public static HttpActionClient client = HttpActionClient.builder() //
            .withUrl("http://en.wikipedia.org/w/") //
            .withUserAgent("ArchetypeBot/1.0 (http://archety.pe/; maxdemarzi@gmail.com)") //
            .build();

    public static MediaWikiBot wikiBot = new MediaWikiBot(client);

    public static String cleanTitle(String url) {
        return url
                .substring( url.lastIndexOf('/')+1, url.length() )
                .replaceAll("_", " ")
                .toLowerCase();
    }

    public static boolean isValidPageLink(String url1, String url2) {
        String title1 = cleanTitle(url1);
        String title2 = cleanTitle(url2);

        Iterator itr = new BacklinkTitles(wikiBot,title1).iterator();
        while(itr.hasNext()) {
            String link = (String)itr.next();
            if(title2.equals(link.toLowerCase())){
                return true;
            }
        }

        itr = new BacklinkTitles(wikiBot,title2).iterator();
        while(itr.hasNext()) {
            String link = (String)itr.next();
            if(title1.equals(link.toLowerCase())){
                return true;
            }
        }

        throw Exception.wikipediaPagesNotLinked;
    }
}
