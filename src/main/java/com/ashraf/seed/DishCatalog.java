package com.ashraf.seed;

import com.ashraf.restaurant.core.enums.FoodType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Loads {@code seed/menu-catalog.csv} ('|' separated, '#' comments) from the classpath. */
public final class DishCatalog {

    private static final String RESOURCE = "/seed/menu-catalog.csv";

    private DishCatalog() {}

    public static List<Dish> load() {
        try (InputStream in = DishCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<Dish> dishes = new ArrayList<>();
            Set<String> names = new HashSet<>();
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] c = line.split("\\|", -1);
                if (c.length != 9) {
                    throw new IllegalStateException(RESOURCE + " line " + lineNo + ": expected 9 columns, got " + c.length);
                }
                try {
                    Dish d = new Dish(
                            c[0].trim(), c[1].trim(), c[2].trim(),
                            Integer.parseInt(c[3].trim()),
                            FoodType.valueOf(c[4].trim()),
                            c[5].trim(),
                            Integer.parseInt(c[6].trim()),
                            MealTime.parse(c[7]),
                            new LinkedHashSet<>(Arrays.asList(c[8].trim().split("\\s*,\\s*"))));
                    if (!names.add(d.name())) {
                        throw new IllegalStateException("duplicate dish name '" + d.name() + "'");
                    }
                    dishes.add(d);
                } catch (RuntimeException e) {
                    throw new IllegalStateException(RESOURCE + " line " + lineNo + ": " + e.getMessage(), e);
                }
            }
            return dishes;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + RESOURCE, e);
        }
    }

    public static Set<String> wikiTitles(Collection<Dish> dishes) {
        Set<String> titles = new LinkedHashSet<>();
        for (Dish d : dishes) titles.add(d.wiki());
        return titles;
    }
}
