package com.ashraf.shared.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");
    private static final int MAX_NAME_PART = 50;

    private Slugs() {}

    /** "Spice Garden" + 7b0f6a52-... -> "spice-garden-7b0f6a". */
    public static String forRestaurant(String name, UUID publicId) {
        String base = slugify(name);
        if (base.isEmpty()) {
            base = "restaurant";
        }
        String code = publicId.toString().replace("-", "").substring(0, 6);
         return base + "-" + code;
    }

    static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        String slug = NON_ALPHANUMERIC.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        if (slug.length() > MAX_NAME_PART) {
            slug = EDGE_DASHES.matcher(slug.substring(0, MAX_NAME_PART)).replaceAll("");
        }
        return slug;
    }
}