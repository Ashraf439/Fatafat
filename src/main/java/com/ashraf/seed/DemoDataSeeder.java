package com.ashraf.seed;

import com.ashraf.commerce.entity.BankDetails;
import com.ashraf.commerce.enums.AddressType;
import com.ashraf.core.entity.Roles;
import com.ashraf.core.entity.User;
import com.ashraf.core.entity.UserRoles;
import com.ashraf.core.enums.Status;
import com.ashraf.core.repository.RolesRepository;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.core.repository.UserRolesRepository;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.entity.RestaurantTimings;
import com.ashraf.restaurant.core.repository.MenuRepository;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import com.ashraf.restaurant.onboarding.entity.TimingSlotEmbeddable;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.restaurant.onboarding.repository.RestaurantOnboardingApplicationRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Seeds realistic demo restaurants (owners, address, bank, timings, onboarding record, menu) with real
 * dish photos uploaded to Cloudinary. Enabled only with {@code seed.demo.enabled=true}.
 *
 * Idempotent: restaurants are keyed by owner email (owner001@..., owner002@..., ...). Re-running skips
 * restaurants that exist and only fills in any images that are still missing.
 */
@Component
@ConditionalOnProperty(name = "seed.demo.enabled", havingValue = "true")
@Order(10) // AdminSeeder is @Order(1): roles and permissions must exist first
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final RestaurantOnboardingApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;
    private final TransactionTemplate tx;

    @Value("${seed.demo.restaurants:100}") private int count;
    @Value("${seed.demo.owner-password:Fatafat@123}") private String ownerPassword;
    @Value("${seed.demo.email-domain:fatafat.test}") private String emailDomain;
    @Value("${seed.demo.images:true}") private boolean withImages;
    @Value("${seed.demo.closed-ratio:0.08}") private double closedRatio;
    @Value("${seed.demo.random-seed:42}") private long randomSeed;
    @Value("${seed.demo.upload-threads:6}") private int uploadThreads;
    @Value("${seed.demo.report-file:seed-output/demo-restaurants.csv}") private String reportFile;
    @Value("${seed.demo.user-agent:FatafatDemoSeeder/1.0 (local development seed; https://github.com/Ashraf439/Fatafat)}")
    private String userAgent;
    @Value("${admin.seed.email}") private String adminEmail;

    public DemoDataSeeder(UserRepository userRepository,
                          RolesRepository rolesRepository,
                          UserRolesRepository userRolesRepository,
                          RestaurantRepository restaurantRepository,
                          MenuRepository menuRepository,
                          RestaurantOnboardingApplicationRepository applicationRepository,
                          PasswordEncoder passwordEncoder,
                          Cloudinary cloudinary,
                          PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.rolesRepository = rolesRepository;
        this.userRolesRepository = userRolesRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuRepository = menuRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinary = cloudinary;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------ orchestration

    private record Uploaded(String url, String publicId) {}

    private record Persisted(long restaurantId, Map<String, Long> menuIdByName,
                             boolean restaurantHasImage, Set<String> menuNamesWithoutImage, boolean created) {}

    private record Result(RestaurantBlueprint blueprint, Long restaurantId, String status,
                          String imageMode, int menuImagesMissing) {}

    @Override
    public void run(String... args) throws Exception {
        long started = System.currentTimeMillis();
        log.info("[seed] Demo data seeding started: {} restaurants, images={}", count, withImages);

        List<Dish> dishes = DishCatalog.load();
        final Map<String, byte[]> photos = withImages ? fetchPhotos(dishes) : Map.of();
        List<Dish> usable = dishes;
        if (withImages) {
            usable = dishes.stream().filter(d -> photos.containsKey(d.wiki())).collect(Collectors.toList());
            log.info("[seed] {} of {} catalog dishes have a downloadable photo", usable.size(), dishes.size());
            if (usable.size() < 60) {
                throw new IllegalStateException("Only " + usable.size() + " dish photos could be downloaded (no internet "
                        + "access to wikipedia.org / wikimedia.org?). Fix connectivity, or run with seed.demo.images=false.");
            }
        }

        List<RestaurantBlueprint> blueprints = BlueprintFactory.build(usable, count, randomSeed, emailDomain, closedRatio);
        Roles restaurantRole = rolesRepository.findByName("RESTAURANT")
                .orElseThrow(() -> new IllegalStateException("RESTAURANT role missing - AdminSeeder must run first"));
        String passwordHash = passwordEncoder.encode(ownerPassword);

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, uploadThreads));
        List<Result> results = new ArrayList<>();
        try {
            for (RestaurantBlueprint bp : blueprints) {
                try {
                    Result r = seedOne(bp, restaurantRole, passwordHash, photos, pool);
                    results.add(r);
                    log.info("[seed] {}/{} {} ({}, {}) -> {}", bp.index(), blueprints.size(), bp.restaurantName(),
                            bp.locality(), bp.city(), r.status());
                } catch (Exception e) {
                    log.error("[seed] {}/{} {} FAILED: {}", bp.index(), blueprints.size(), bp.restaurantName(), e.toString());
                    results.add(new Result(bp, null, "FAILED: " + e.getMessage(), "-", -1));
                }
            }
        } finally {
            pool.shutdown();
        }

        writeReport(results);
        long created = results.stream().filter(r -> r.status().startsWith("CREATED")).count();
        long failed = results.stream().filter(r -> r.status().startsWith("FAILED")).count();
        long missing = results.stream().filter(r -> r.menuImagesMissing() > 0).mapToInt(Result::menuImagesMissing).sum();
        log.info("[seed] Done in {}s: created={}, alreadyExisting={}, failed={}, menuItemsStillWithoutImage={}. "
                        + "Owner login list: {} (password: {})",
                (System.currentTimeMillis() - started) / 1000, created, results.size() - created - failed, failed,
                missing, reportFile, ownerPassword);
        if (failed > 0 || missing > 0) {
            log.warn("[seed] Some items failed. Just restart with the same settings: existing restaurants are skipped "
                    + "and missing images are filled in.");
        }
    }

    // ------------------------------------------------------------------ one restaurant

    private Result seedOne(RestaurantBlueprint bp, Roles role, String passwordHash,
                           Map<String, byte[]> photos, ExecutorService pool) {
        Persisted p = loadExisting(bp);
        if (p == null) p = persistNew(bp, role, passwordHash);

        int missing = 0;
        String imageMode = "-";
        if (withImages) {
            final Persisted persisted = p;
            final long restaurantId = p.restaurantId();
            Map<Long, Uploaded> uploads = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int needed = 0;

            for (RestaurantBlueprint.Line line : bp.menu()) {
                String dishName = line.dish().name();
                if (!persisted.menuNamesWithoutImage().contains(dishName)) continue;
                needed++;
                Long menuId = persisted.menuIdByName().get(dishName);
                byte[] bytes = photos.get(line.dish().wiki());
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        uploads.put(menuId, upload(bytes, "menu/" + restaurantId));
                    } catch (Exception e) {
                        log.warn("[seed] menu image upload failed for '{}': {}", dishName, e.toString());
                    }
                }, pool));
            }

            Uploaded restaurantImage = null;
            if (!persisted.restaurantHasImage()) {
                try {
                    List<RestaurantBlueprint.Line> heroes = bp.heroLines();
                    List<byte[]> heroBytes = new ArrayList<>();
                    for (RestaurantBlueprint.Line h : heroes) heroBytes.add(photos.get(h.dish().wiki()));
                    byte[] image = heroBytes.get(0);
                    imageMode = "dish:" + heroes.get(0).dish().name();
                    if (heroBytes.size() > 1) {
                        byte[] collage = CollageBuilder.collage(heroBytes);
                        if (collage != null) {
                            image = collage;
                            imageMode = "collage:" + heroes.stream().map(h -> h.dish().name()).collect(Collectors.joining(" + "));
                        }
                    }
                    restaurantImage = upload(image, "restaurants/" + restaurantId);
                } catch (Exception e) {
                    log.warn("[seed] restaurant image failed for '{}': {}", bp.restaurantName(), e.toString());
                    imageMode = "FAILED";
                }
            } else {
                imageMode = "existing";
            }

            futures.forEach(CompletableFuture::join);
            final Uploaded finalRestaurantImage = restaurantImage;
            if (!uploads.isEmpty() || finalRestaurantImage != null) {
                tx.executeWithoutResult(status -> {
                    for (Map.Entry<Long, Uploaded> e : uploads.entrySet()) {
                        Menu m = menuRepository.findById(e.getKey()).orElseThrow();
                        m.setImageUrl(e.getValue().url());
                        m.setImagePublicId(e.getValue().publicId());
                        menuRepository.save(m);
                    }
                    if (finalRestaurantImage != null) {
                        Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow();
                        r.setImageUrl(finalRestaurantImage.url());
                        r.setImagePublicId(finalRestaurantImage.publicId());
                        restaurantRepository.save(r);
                    }
                });
            }
            missing = needed - uploads.size();
        }
        return new Result(bp, p.restaurantId(), (p.created() ? "CREATED" : "EXISTING") + (missing > 0 ? " (" + missing + " images missing)" : ""),
                imageMode, missing);
    }

    /** If this owner already exists from a previous run, load what we need to top up images. */
    private Persisted loadExisting(RestaurantBlueprint bp) {
        Optional<User> owner = userRepository.findByEmail(bp.ownerEmail());
        if (owner.isEmpty()) return null;
        Restaurant r = restaurantRepository.findByOwnerUser_Id(owner.get().getId())
                .orElseThrow(() -> new IllegalStateException("User " + bp.ownerEmail()
                        + " exists but has no restaurant; delete that user and re-run"));
        Map<String, Long> ids = new HashMap<>();
        Set<String> withoutImage = new HashSet<>();
        for (Menu m : menuRepository.findByRestaurant_Id(r.getId())) {
            ids.put(m.getDishName(), m.getId());
            if (m.getImageUrl() == null) withoutImage.add(m.getDishName());
        }
        return new Persisted(r.getId(), ids, r.getImageUrl() != null, withoutImage, false);
    }

    private Persisted persistNew(RestaurantBlueprint bp, Roles role, String passwordHash) {
        return tx.execute(status -> {
            User owner = new User();
            owner.setEmail(bp.ownerEmail());
            owner.setPasswordHash(passwordHash);
            owner.setStatus(Status.ACTIVE);
            userRepository.save(owner);

            UserRoles userRole = new UserRoles();
            userRole.setUser(owner);
            userRole.setRole(role);
            userRolesRepository.save(userRole);

            Restaurant r = new Restaurant();
            r.setOwnerUser(owner);
            r.setName(bp.restaurantName());
            r.setOwnerName(bp.ownerName());
            r.setFssaiLicense(bp.fssai());
            r.setGstin(bp.gstin());
            r.setIsOpen(bp.open());
            r.setRestaurantOnboardingStatus(RestaurantOnboardingStatus.LIVE);

            RestaurantAddress address = new RestaurantAddress();
            address.setStreet(bp.street());
            address.setLandmark(bp.landmark());
            address.setCity(bp.city());
            address.setState(bp.state());
            address.setPincode(bp.pincode());
            address.setLatitude(bp.latitude());
            address.setLongitude(bp.longitude());
            address.setCountry("India");
            address.setAddressType(AddressType.OTHER);
            address.setIsDefault(true);
            address.setRestaurant(r);
            r.getAddresses().add(address);

            BankDetails bank = new BankDetails();
            bank.setRestaurant(r);
            bank.setAccountHolderName(bp.ownerName());
            bank.setAccountNumber(bp.accountNumber());
            bank.setIfscCode(bp.ifsc());
            bank.setBankName(bp.bankName());
            r.setBankDetails(bank);

            for (RestaurantBlueprint.Slot s : bp.timings()) {
                RestaurantTimings t = new RestaurantTimings();
                t.setRestaurant(r);
                t.setDayOfWeek(s.day());
                t.setOpenTime(s.open());
                t.setCloseTime(s.close());
                r.getTimings().add(t);
            }

            for (RestaurantBlueprint.Line line : bp.menu()) {
                Dish d = line.dish();
                Menu m = new Menu();
                m.setRestaurant(r);
                m.setDishName(d.name());
                m.setDescription(d.description());
                m.setPrice(line.price());
                m.setFoodType(d.foodType());
                m.setCategory(d.category());
                m.setPreparationTimeMinutes(line.prepMinutes());
                r.getMenuItems().add(m);
            }
            Restaurant saved = restaurantRepository.saveAndFlush(r);

            // Onboarding record so /applications/me shows this restaurant as LIVE, like a real onboarded partner.
            RestaurantOnboardingApplication app = new RestaurantOnboardingApplication();
            app.setUser(owner);
            app.setAttemptNumber(1);
            app.setRestaurantName(bp.restaurantName());
            app.setOwnerName(bp.ownerName());
            app.setAddressLine(bp.street());
            app.setCity(bp.city());
            app.setState(bp.state());
            app.setPincode(bp.pincode());
            app.setFssaiLicense(bp.fssai());
            app.setGstin(bp.gstin());
            app.setAccountHolderName(bp.ownerName());
            app.setAccountNumber(bp.accountNumber());
            app.setIfscCode(bp.ifsc());
            app.setBankName(bp.bankName());
            app.setStatus(RestaurantOnboardingStatus.LIVE);
            app.setReviewedAt(LocalDateTime.now());
            userRepository.findByEmail(adminEmail).ifPresent(app::setReviewedByAdmin);
            for (RestaurantBlueprint.Slot s : bp.timings()) {
                TimingSlotEmbeddable e = new TimingSlotEmbeddable();
                e.setDayOfWeek(s.day());
                e.setOpenTime(s.open());
                e.setCloseTime(s.close());
                app.getTimings().add(e);
            }
            applicationRepository.save(app);

            Map<String, Long> ids = new HashMap<>();
            for (Menu m : saved.getMenuItems()) ids.put(m.getDishName(), m.getId());
            return new Persisted(saved.getId(), ids, false, new HashSet<>(ids.keySet()), true);
        });
    }

    // ------------------------------------------------------------------ images

    private Map<String, byte[]> fetchPhotos(List<Dish> dishes) throws Exception {
        WikiImageClient wiki = new WikiImageClient(userAgent);
        Set<String> titles = DishCatalog.wikiTitles(dishes);
        log.info("[seed] Resolving {} dish photos on Wikipedia...", titles.size());
        Map<String, String> urls = wiki.resolveThumbnails(titles);
        for (String t : titles) {
            if (!urls.containsKey(t)) log.warn("[seed] no Wikipedia photo for '{}' - dishes using it are skipped", t);
        }

        Map<String, byte[]> bytes = new ConcurrentHashMap<>();
        ExecutorService downloads = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Map.Entry<String, String> e : urls.entrySet()) {
                futures.add(downloads.submit(() -> {
                    try {
                        bytes.put(e.getKey(), wiki.download(e.getValue()));
                    } catch (Exception ex) {
                        log.warn("[seed] download failed for '{}': {}", e.getKey(), ex.toString());
                    }
                }));
            }
            for (Future<?> f : futures) f.get();
        } finally {
            downloads.shutdown();
        }
        log.info("[seed] Downloaded {} photos", bytes.size());
        return bytes;
    }

    private Uploaded upload(byte[] bytes, String folder) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = cloudinary.uploader().upload(bytes,
                        ObjectUtils.asMap("folder", folder, "resource_type", "image"));
                return new Uploaded((String) result.get("secure_url"), (String) result.get("public_id"));
            } catch (Exception e) {
                last = e;
                Thread.sleep(1000L * attempt);
            }
        }
        throw last;
    }

    // ------------------------------------------------------------------ report

    private void writeReport(List<Result> results) {
        try {
            Path path = Path.of(reportFile);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            StringBuilder sb = new StringBuilder(
                    "index,restaurantId,restaurantName,cuisine,city,locality,servesMeals,isOpen,menuItems,restaurantImage,ownerEmail,ownerPassword,status\n");
            for (Result r : results) {
                RestaurantBlueprint b = r.blueprint();
                sb.append(String.join(",",
                        String.valueOf(b.index()), r.restaurantId() == null ? "" : String.valueOf(r.restaurantId()),
                        csv(b.restaurantName()), csv(b.cuisine()), csv(b.city()), csv(b.locality()),
                        csv(b.meals().stream().map(Enum::name).collect(Collectors.joining("+"))),
                        String.valueOf(b.open()), String.valueOf(b.menu().size()), csv(r.imageMode()),
                        csv(b.ownerEmail()), csv(ownerPassword), csv(r.status()))).append('\n');
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
            log.info("[seed] Wrote {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[seed] Could not write report file {}: {}", reportFile, e.toString());
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
