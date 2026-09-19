package com.ashraf.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/**
 * Deterministically generates restaurant blueprints (same random-seed => same restaurants), so re-running
 * the seeder is idempotent. Pure Java, no Spring, no I/O.
 */
public final class BlueprintFactory {

    private BlueprintFactory() {}

    // ------------------------------------------------------------------ static data

    private record Locality(String name, String pincode) {}

    private record City(String name, String state, String gstCode, double lat, double lng,
                        double priceFactor, int weight, List<Locality> localities) {}

    private record Archetype(String key, String cuisine, Set<String> tags, int share,
                             String[] patterns, String[] prefixes, String[] suffixes, String[] noBreakfastSuffixes) {}

    private static Locality l(String n, String p) { return new Locality(n, p); }

    private static final List<City> CITIES = List.of(
            new City("Vijayawada", "Andhra Pradesh", "37", 16.5062, 80.6480, 1.00, 30, List.of(
                    l("Benz Circle", "520010"), l("Governorpet", "520002"), l("Labbipet", "520010"),
                    l("Auto Nagar", "520007"), l("Patamata", "520010"), l("MG Road", "520010"),
                    l("Gunadala", "520004"), l("Bhavanipuram", "520012"))),
            new City("Guntur", "Andhra Pradesh", "37", 16.3067, 80.4365, 0.95, 15, List.of(
                    l("Brodipet", "522002"), l("Arundelpet", "522002"), l("Lakshmipuram", "522007"),
                    l("Kothapet", "522001"), l("Nagarampalem", "522004"), l("Pattabhipuram", "522006"))),
            new City("Tadepalle", "Andhra Pradesh", "37", 16.4740, 80.6060, 0.95, 10, List.of(
                    l("Tadepalle Main Road", "522501"), l("Undavalli Centre", "522501"), l("Kuchipudi Road", "522501"))),
            new City("Mangalagiri", "Andhra Pradesh", "37", 16.4307, 80.5525, 0.92, 5, List.of(
                    l("Mangalagiri Main Road", "522503"), l("Autonagar Mangalagiri", "522503"))),
            new City("Hyderabad", "Telangana", "36", 17.3850, 78.4867, 1.15, 20, List.of(
                    l("Madhapur", "500081"), l("Kondapur", "500084"), l("Gachibowli", "500032"),
                    l("Banjara Hills", "500034"), l("Jubilee Hills", "500033"), l("Ameerpet", "500016"),
                    l("Kukatpally", "500072"), l("Begumpet", "500016"), l("Tolichowki", "500008"))),
            new City("Bengaluru", "Karnataka", "29", 12.9716, 77.5946, 1.15, 20, List.of(
                    l("Indiranagar", "560038"), l("Koramangala", "560034"), l("HSR Layout", "560102"),
                    l("Jayanagar", "560011"), l("Malleshwaram", "560003"), l("BTM Layout", "560076"),
                    l("Marathahalli", "560037"), l("Whitefield", "560066"))));

    private static final List<Archetype> ARCHETYPES = List.of(
            new Archetype("SI", "South Indian Tiffins", Set.of("SI"), 15,
                    new String[]{"B", "B", "BD", "BD", "BL", "BLD", "BLD", "L"},
                    new String[]{"Sri Sai", "Sri Lakshmi", "Vaishnavi", "Udupi", "Anjaneya", "Sri Krishna", "Ganesh",
                            "Balaji", "Venkateswara", "Annapurna", "Sri Ram", "Mahalakshmi", "Saraswati", "Nandini",
                            "Raghavendra", "Sri Durga"},
                    new String[]{"Tiffins", "Bhavan", "Idli House", "Dosa Corner", "Vilas", "Cafe", "Tiffin Centre"},
                    new String[]{"Meals", "Mess", "Bhojanam", "Meals Hotel"}),
            new Archetype("AND", "Andhra Meals & Curries", Set.of("AND"), 10,
                    new String[]{"L", "L", "LD", "LD", "LD"},
                    new String[]{"Guntur", "Godavari", "Rayalaseema", "Nellore", "Kakinada", "Konaseema", "Amaravati",
                            "Krishna", "Telugu", "Kaveri", "Andhra"},
                    new String[]{"Ruchulu", "Vindu", "Bhojanam", "Spice Kitchen", "Family Restaurant", "Meals Point"},
                    null),
            new Archetype("BIR", "Biryani & Kebabs", Set.of("BIR"), 12,
                    new String[]{"LD", "LD", "LD", "D", "L"},
                    new String[]{"Nizami", "Golconda", "Charminar", "Deccan", "Falaknuma", "Shahi", "Mughlai", "Royal Dum",
                            "Awadhi", "Zaika", "Bismillah", "Al Noor"},
                    new String[]{"Biryani House", "Biryani Point", "Kebabs & Biryani", "Dum Biryani", "Biryani Darbar"},
                    null),
            new Archetype("NI", "North Indian", Set.of("NI"), 15,
                    new String[]{"LD", "LD", "LD", "BLD", "BLD", "D", "L"},
                    new String[]{"Punjabi", "Amritsari", "Delhi", "Lucknowi", "Tandoori", "Royal", "Mughal", "Sardar",
                            "Rajdhani", "Maharaja", "Desi", "Jaipur"},
                    new String[]{"Tadka", "Dhaba", "Darbar", "Rasoi", "Kitchen", "Junction", "Handi", "Tandoor"},
                    null),
            new Archetype("CHI", "Indo-Chinese", Set.of("CHI"), 8,
                    new String[]{"LD", "LD", "D", "L"},
                    new String[]{"Dragon", "Golden", "Wok", "Red Chilli", "China Town", "Noodle", "Bamboo", "Lucky",
                            "Ming", "Schezwan", "Hakka"},
                    new String[]{"Wok", "Kitchen", "Express", "Chinese Corner", "House", "Garden", "Bowl"},
                    null),
            new Archetype("STR", "Street Food & Chaat", Set.of("STR"), 8,
                    new String[]{"LD", "LD", "D", "L"},
                    new String[]{"Chatpata", "Mumbai", "Delhi", "Bombay", "Gully", "Nukkad", "Zaika", "Desi", "Masala",
                            "Tikki"},
                    new String[]{"Chaat Corner", "Chaat Bhandar", "Street Bites", "Nukkad", "Snacks Point", "Chowk"},
                    null),
            new Archetype("BAK", "Bakery & Cafe", Set.of("BAK"), 13,
                    new String[]{"B", "B", "BL", "BL", "BLD", "BLD", "L"},
                    new String[]{"Crumb", "Butterfly", "Sunrise", "Morning Glory", "Oven Fresh", "Sugar Spoon",
                            "Little Bakes", "Bean & Bloom", "Toasty", "Whisk", "Golden Crust", "Brew & Bite",
                            "Cinnamon", "Maple"},
                    new String[]{"Bakery", "Cafe", "Bake Shop", "Coffee House", "Kitchen & Cafe"},
                    null),
            new Archetype("FF", "Burgers & Fast Food", Set.of("FF"), 8,
                    new String[]{"LD", "LD", "D", "L"},
                    new String[]{"Burger", "Crispy", "Big Bite", "Grill", "Fry", "Bun", "Fast Lane", "Cheesy", "Patty",
                            "Snack"},
                    new String[]{"Junction", "Hub", "Factory", "Express", "Shack", "Point"},
                    null),
            new Archetype("ITA", "Italian", Set.of("ITA"), 6,
                    new String[]{"LD", "LD", "D"},
                    new String[]{"La Piazza", "Bella", "Mamma's", "Roma", "Toscana", "Napoli", "Il Forno", "Pasta Amore",
                            "Dolce Vita"},
                    new String[]{"Trattoria", "Kitchen", "Pizzeria", "Osteria", "Cucina"},
                    null),
            new Archetype("KER", "Kerala Kitchen", Set.of("KER"), 5,
                    new String[]{"BLD", "BLD", "LD", "BD"},
                    new String[]{"Kerala", "Malabar", "Kochi", "Alleppey", "Travancore", "Kuttanad", "Thalassery", "Kovalam"},
                    new String[]{"Kitchen", "Mess", "Ruchi", "Sadya House", "Family Restaurant", "Spice Kitchen"},
                    null));

    private static final String[] FIRST = {"Ravi", "Suresh", "Venkat", "Srinivas", "Ramesh", "Anil", "Kiran", "Mahesh",
            "Prasad", "Naresh", "Lakshmi", "Padma", "Sunitha", "Anitha", "Rajesh", "Sandeep", "Harish", "Vijay",
            "Mohammed", "Imran", "Farooq", "Arjun", "Karthik", "Manoj", "Deepak", "Pradeep", "Sai", "Krishna", "Rohit"};
    private static final String[] LAST = {"Reddy", "Naidu", "Rao", "Chowdary", "Sastry", "Prasad", "Varma", "Raju",
            "Kumar", "Sharma", "Gupta", "Singh", "Iyer", "Nair", "Khan", "Shaik", "Yadav", "Patel", "Babu", "Murthy"};
    private static final String[] LANDMARKS = {"Near Bus Stand", "Opposite Petrol Bunk", "Beside Pharmacy",
            "Near Rythu Bazar", "Opposite City Hospital", "Near Main Circle", "Behind Cinema Hall", "Near Railway Station"};
    private static final String[][] BANKS = {{"State Bank of India", "SBIN"}, {"HDFC Bank", "HDFC"},
            {"ICICI Bank", "ICIC"}, {"Axis Bank", "UTIB"}, {"Canara Bank", "CNRB"}, {"Union Bank of India", "UBIN"}};

    // ------------------------------------------------------------------ public API

    public static List<RestaurantBlueprint> build(List<Dish> dishes, int count, long seed,
                                                  String emailDomain, double closedRatio) {
        List<Archetype> slots = new ArrayList<>();
        for (Archetype a : ARCHETYPES) for (int i = 0; i < a.share(); i++) slots.add(a);
        Collections.shuffle(slots, new Random(seed));

        int totalWeight = CITIES.stream().mapToInt(City::weight).sum();
        Set<String> usedNames = new HashSet<>();
        List<RestaurantBlueprint> result = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Archetype a = slots.get((i - 1) % slots.size());
            Random rnd = new Random(seed * 1_000_003L + i);

            Set<MealTime> served = MealTime.parse(a.patterns()[rnd.nextInt(a.patterns().length)]);
            City city = pickCity(rnd, totalWeight);
            Locality loc = city.localities().get(rnd.nextInt(city.localities().size()));

            String name = uniqueName(a, served, loc, i, rnd, usedNames);
            String owner = FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)];
            boolean premium = rnd.nextDouble() < 0.10;

            List<RestaurantBlueprint.Line> menu = new ArrayList<>();
            for (Dish d : selectMenu(dishes, a, served, rnd)) {
                menu.add(new RestaurantBlueprint.Line(d, price(d.price(), city, premium, rnd),
                        d.prepMinutes() + rnd.nextInt(4)));
            }

            String[] bank = BANKS[rnd.nextInt(BANKS.length)];
            result.add(new RestaurantBlueprint(
                    i, a.cuisine(),
                    String.format("owner%03d@%s", i, emailDomain), owner, name,
                    city.name(), city.state(), loc.name(),
                    "Shop No. " + (1 + rnd.nextInt(120)) + ", " + loc.name() + " Main Road",
                    loc.pincode(),
                    coord(city.lat(), rnd), coord(city.lng(), rnd),
                    LANDMARKS[rnd.nextInt(LANDMARKS.length)],
                    fssai(city, rnd), gstin(city, rnd),
                    bank[0], bank[1] + "0" + digits(rnd, 6), digits(rnd, 12),
                    rnd.nextDouble() >= closedRatio,
                    served, timings(served, rnd), menu));
        }
        return result;
    }

    // ------------------------------------------------------------------ helpers

    private static City pickCity(Random rnd, int totalWeight) {
        int roll = rnd.nextInt(totalWeight);
        for (City c : CITIES) {
            roll -= c.weight();
            if (roll < 0) return c;
        }
        return CITIES.get(0);
    }

    private static String uniqueName(Archetype a, Set<MealTime> served, Locality loc, int index,
                                     Random rnd, Set<String> used) {
        String[] suffixes = (a.noBreakfastSuffixes() != null && !served.contains(MealTime.BREAKFAST))
                ? a.noBreakfastSuffixes() : a.suffixes();
        for (int attempt = 0; attempt < 30; attempt++) {
            String n = a.prefixes()[rnd.nextInt(a.prefixes().length)] + " " + suffixes[rnd.nextInt(suffixes.length)];
            if (used.add(n)) return n;
        }
        String withLocality = a.prefixes()[0] + " " + suffixes[0] + " - " + loc.name();
        if (used.add(withLocality)) return withLocality;
        String last = withLocality + " " + index;
        used.add(last);
        return last;
    }

    /** 10-15 dishes: >=3 per served meal, at least one beverage and dessert if available, no duplicate photos. */
    private static List<Dish> selectMenu(List<Dish> all, Archetype a, Set<MealTime> served, Random rnd) {
        List<Dish> eligible = new ArrayList<>();
        for (Dish d : all) {
            if (!Collections.disjoint(d.tags(), a.tags()) && !Collections.disjoint(d.meals(), served)) eligible.add(d);
        }
        Collections.shuffle(eligible, rnd);
        int target = 10 + rnd.nextInt(6);

        Map<String, Dish> chosen = new LinkedHashMap<>(); // keyed by photo so one restaurant never shows the same photo twice
        for (MealTime meal : served) {
            long have = chosen.values().stream().filter(d -> d.meals().contains(meal)).count();
            for (Dish d : eligible) {
                if (have >= 3) break;
                if (d.meals().contains(meal) && !chosen.containsKey(d.wiki())) {
                    chosen.put(d.wiki(), d);
                    have++;
                }
            }
        }
        for (String category : List.of("Beverages", "Desserts")) {
            if (chosen.size() >= target) break;
            if (chosen.values().stream().noneMatch(d -> d.category().equals(category))) {
                for (Dish d : eligible) {
                    if (d.category().equals(category) && !chosen.containsKey(d.wiki())) {
                        chosen.put(d.wiki(), d);
                        break;
                    }
                }
            }
        }
        for (Dish d : eligible) {
            if (chosen.size() >= target) break;
            chosen.putIfAbsent(d.wiki(), d);
        }
        List<Dish> menu = new ArrayList<>(chosen.values());
        menu.sort(Comparator.comparing(Dish::category)); // group by category like a real menu
        return menu;
    }

    private static BigDecimal price(int base, City city, boolean premium, Random rnd) {
        double factor = city.priceFactor() * (premium ? 1.25 : 1.0) * (0.92 + rnd.nextDouble() * 0.20);
        int rounded = Math.max(20, (int) (Math.round(base * factor / 5.0) * 5));
        return BigDecimal.valueOf(rounded).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static List<RestaurantBlueprint.Slot> timings(Set<MealTime> served, Random rnd) {
        DayOfWeek closedDay = null;
        if (rnd.nextDouble() < 0.20) {
            closedDay = rnd.nextDouble() < 0.7 ? DayOfWeek.MONDAY : DayOfWeek.of(1 + rnd.nextInt(7));
        }
        // open jitter 0-30 min later, close jitter 0-30 min earlier: shifts can never overlap
        Map<MealTime, LocalTime[]> windows = new EnumMap<>(MealTime.class);
        for (MealTime m : served) {
            windows.put(m, new LocalTime[]{
                    m.defaultOpen().plusMinutes(15L * rnd.nextInt(3)),
                    m.defaultClose().minusMinutes(15L * rnd.nextInt(3))});
        }
        List<RestaurantBlueprint.Slot> slots = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day == closedDay) continue;
            for (Map.Entry<MealTime, LocalTime[]> e : windows.entrySet()) {
                slots.add(new RestaurantBlueprint.Slot(day, e.getValue()[0], e.getValue()[1]));
            }
        }
        return slots;
    }

    private static BigDecimal coord(double center, Random rnd) {
        return BigDecimal.valueOf(center + (rnd.nextDouble() - 0.5) * 0.06).setScale(7, RoundingMode.HALF_UP);
    }

    private static String digits(Random rnd, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(rnd.nextInt(10));
        return sb.toString();
    }

    private static char letter(Random rnd) { return (char) ('A' + rnd.nextInt(26)); }

    /** Plausible-looking (NOT real) 14-digit FSSAI number. */
    private static String fssai(City city, Random rnd) {
        return "1" + city.gstCode() + digits(rnd, 11);
    }

    /** Plausible-looking (NOT real, checksum not valid) 15-character GSTIN. */
    private static String gstin(City city, Random rnd) {
        String pan = "" + letter(rnd) + letter(rnd) + letter(rnd) + "CF".charAt(rnd.nextInt(2)) + letter(rnd)
                + digits(rnd, 4) + letter(rnd);
        return city.gstCode() + pan + "1Z" + (rnd.nextBoolean() ? letter(rnd) : (char) ('0' + rnd.nextInt(10)));
    }
}
