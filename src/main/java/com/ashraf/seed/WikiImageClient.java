package com.ashraf.seed;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Resolves Wikipedia article titles to their lead-photo thumbnail and downloads the bytes.
 * Uses the public MediaWiki API (no key). Wikimedia asks clients to send a descriptive User-Agent.
 * Note: the MediaWiki API accepts at most 50 titles per request; we use 40.
 */
public class WikiImageClient {

    private static final String API = "https://en.wikipedia.org/w/api.php?action=query&format=json"
            + "&formatversion=2&redirects=1&prop=pageimages&piprop=thumbnail&pithumbsize=500&titles=";
    private static final int BATCH = 40;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final String userAgent;

    public WikiImageClient(String userAgent) {
        this.userAgent = userAgent;
    }

    /** @return requested title -> thumbnail URL. Titles without a lead photo are simply absent. */
    public Map<String, String> resolveThumbnails(Collection<String> titles) throws IOException, InterruptedException {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> all = new ArrayList<>(titles);
        for (int from = 0; from < all.size(); from += BATCH) {
            List<String> chunk = all.subList(from, Math.min(all.size(), from + BATCH));
            String url = API + URLEncoder.encode(String.join("|", chunk), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(getString(url));
            if (root.has("error")) {
                throw new IOException("Wikipedia API error: " + root.getJSONObject("error").optString("info"));
            }
            JSONObject query = root.getJSONObject("query");
            Map<String, String> normalized = pairs(query.optJSONArray("normalized"));
            Map<String, String> redirects = pairs(query.optJSONArray("redirects"));

            Map<String, String> thumbByTitle = new HashMap<>();
            JSONArray pages = query.optJSONArray("pages");
            if (pages != null) {
                for (int i = 0; i < pages.length(); i++) {
                    JSONObject page = pages.getJSONObject(i);
                    JSONObject thumb = page.optJSONObject("thumbnail");
                    if (thumb != null) thumbByTitle.put(page.getString("title"), thumb.getString("source"));
                }
            }
            for (String requested : chunk) {
                String t = normalized.getOrDefault(requested, requested);
                for (int hop = 0; hop < 3 && redirects.containsKey(t); hop++) t = redirects.get(t);
                String thumb = thumbByTitle.get(t);
                if (thumb != null) result.put(requested, thumb);
            }
            Thread.sleep(200); // be polite
        }
        return result;
    }

    /** Downloads an image, retrying on 429/5xx with backoff. */
    public byte[] download(String url) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            HttpResponse<byte[]> resp = http.send(request(url), HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            if (status == 200) {
                String type = resp.headers().firstValue("content-type").orElse("");
                if (!type.startsWith("image/")) throw new IOException("Not an image (" + type + "): " + url);
                return resp.body();
            }
            last = new IOException("HTTP " + status + " for " + url);
            if (status == 429 || status >= 500) {
                long waitSeconds = resp.headers().firstValue("retry-after").map(Long::parseLong)
                        .orElse((long) Math.pow(2, attempt));
                Thread.sleep(Math.min(waitSeconds, 30) * 1000L);
            } else {
                break;
            }
        }
        throw last;
    }

    private String getString(String url) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(request(url), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        return resp.body();
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .GET().build();
    }

    private static Map<String, String> pairs(JSONArray arr) {
        Map<String, String> map = new HashMap<>();
        if (arr == null) return map;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            map.put(o.getString("from"), o.getString("to"));
        }
        return map;
    }
}
