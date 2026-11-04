package com.ashraf.restaurant.onboarding.service;

import com.ashraf.commerce.entity.BankDetails;
import com.ashraf.core.entity.User;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.payment.OrderResult;
import com.ashraf.payment.entity.OnboardingPayment;
import com.ashraf.payment.enums.OnboardingPaymentStatus;
import com.ashraf.payment.repository.OnboardingPaymentRepository;
import com.ashraf.payment.service.PaymentService;
import com.ashraf.restaurant.core.dto.TimingSlotRequest;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.entity.RestaurantTimings;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.core.repository.RestaurantTimingsRepository;
import com.ashraf.restaurant.core.util.TimingSlotValidator;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.dto.OwnerEntitlementResponse;
import com.ashraf.restaurant.onboarding.dto.RejectApplicationRequest;
import com.ashraf.restaurant.onboarding.dto.RestaurantOnboardingApplicationRequest;
import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import com.ashraf.restaurant.onboarding.entity.TimingSlotEmbeddable;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.restaurant.onboarding.repository.RestaurantOnboardingApplicationRepository;
import com.ashraf.shared.exception.AccessDeniedException;
import com.ashraf.shared.exception.ApplicationAlreadyActiveException;
import com.ashraf.shared.exception.ApplicationNotFoundException;
import com.ashraf.shared.exception.InvalidApplicationStateException;
import com.ashraf.shared.exception.OnboardingFeeAlreadyPaidException;
import com.ashraf.shared.exception.PaymentNotFoundException;
import com.ashraf.shared.exception.RestaurantQuotaExceededException;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RestaurantOnboardingService {

    /** Statuses that occupy one of the owner's restaurant-quota slots. REJECTED frees the slot back up. */
    private static final List<RestaurantOnboardingStatus> QUOTA_OCCUPYING_STATUSES = List.of(
            RestaurantOnboardingStatus.UNDER_REVIEW,
            RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT,
            RestaurantOnboardingStatus.LIVE);

    /** How many restaurants a single onboarding-fee payment unlocks (the first one + this many more). */
    private static final int RESTAURANT_QUOTA_ON_PAYMENT = 4;

    private final RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTimingsRepository restaurantTimingsRepository;
    private final OnboardingPaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @Value("${onboarding.fee.amount}")
    private double onboardingFeeAmount;

    public RestaurantOnboardingService(
            RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository,
            RestaurantRepository restaurantRepository,
            RestaurantTimingsRepository restaurantTimingsRepository,
            OnboardingPaymentRepository paymentRepository,
            PaymentService paymentService,
            UserRepository userRepository) {
        this.restaurantOnboardingApplicationRepository = restaurantOnboardingApplicationRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantTimingsRepository = restaurantTimingsRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    /**
     * Only the owner role may apply for / pay for a restaurant. A staff account (manager, staff,
     * cashier) is scoped to the restaurant it was added to via RestaurantStaffService — it's never
     * meant to independently onboard its own restaurant, even though /api/restaurant/** is open to
     * all four roles for the day-to-day endpoints.
     */
    private boolean isOwnerAccount(User user) {
        return user.getUserRoles() != null && user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null && "RESTAURANT".equals(ur.getRole().getName()));
    }

    @Transactional
    public void submitApplication(User user, RestaurantOnboardingApplicationRequest request) {
        if (!isOwnerAccount(user)) {
            throw new AccessDeniedException("Only restaurant owners can submit an onboarding application.");
        }

        boolean hasActiveApplication = restaurantOnboardingApplicationRepository
                .existsByUser_IdAndStatusIn(user.getId(), List.of(
                        RestaurantOnboardingStatus.UNDER_REVIEW,
                        RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT));

        if (hasActiveApplication) {
            throw new ApplicationAlreadyActiveException(
                    "You already have an application under review or awaiting payment.");
        }

        // Lock the owner row so two concurrent submissions can't both read "2 of 4 used" and
        // both proceed, ending up with 5 restaurants. Only matters once they've paid (quota > 0);
        // a never-paid owner is always allowed exactly one in-flight application (the check above
        // already guarantees that), so no lock is needed for their very first application.
        if (user.getOnboardingFeePaidAt() != null) {
            User locked = userRepository.lockById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User vanished mid-transaction: " + user.getId()));

            long occupiedSlots = restaurantOnboardingApplicationRepository
                    .countByUser_IdAndStatusIn(locked.getId(), QUOTA_OCCUPYING_STATUSES);

            if (occupiedSlots >= locked.getRestaurantQuota()) {
                throw new RestaurantQuotaExceededException(
                        "You've reached your limit of " + locked.getRestaurantQuota()
                                + " restaurants. Remove or wait on an existing one before adding another.");
            }
        }

        int nextAttemptNumber = restaurantOnboardingApplicationRepository
                .findByUser_IdOrderByAttemptNumberDesc(user.getId()).stream()
                .findFirst()
                .map(app -> app.getAttemptNumber() + 1)
                .orElse(1);

        TimingSlotValidator.validate(request.getTimings());

        RestaurantOnboardingApplication application = new RestaurantOnboardingApplication();
        application.setUser(user);
        application.setAttemptNumber(nextAttemptNumber);
        application.setRestaurantName(request.getRestaurantName());
        application.setOwnerName(request.getOwnerName());
        application.setAddressLine(request.getAddressLine());
        application.setCity(request.getCity());
        application.setState(request.getState());
        application.setPincode(request.getPincode());
        application.setFssaiLicense(request.getFssaiLicense());
        application.setGstin(request.getGstin());
        application.setAccountHolderName(request.getAccountHolderName());
        application.setAccountNumber(request.getAccountNumber());
        application.setBankName(request.getBankName());
        application.setIfscCode(request.getIfscCode());
        application.setTimings(new ArrayList<>(
                request.getTimings().stream().map(this::toEmbeddable).toList()));
        application.setStatus(RestaurantOnboardingStatus.UNDER_REVIEW);

        restaurantOnboardingApplicationRepository.save(application);
    }

    /**
     * Admin approval. A never-paid owner's first restaurant still needs the one-time onboarding
     * fee, so it moves to APPROVED_PENDING_PAYMENT as before. An owner who has already paid (this
     * is restaurant #2-#4) skips straight to LIVE — there's nothing left to pay for.
     */
    @Transactional
    public RestaurantOnboardingApplication approveApplication(UUID applicationId, User admin) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findByPublicId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (application.getStatus() != RestaurantOnboardingStatus.UNDER_REVIEW) {
            throw new InvalidApplicationStateException(
                    "Only applications under review can be approved. Current status: " + application.getStatus());
        }

        application.setReviewedByAdmin(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(null);

        if (application.getUser().getOnboardingFeePaidAt() != null) {
            createRestaurantFromApplication(application);
            application.setStatus(RestaurantOnboardingStatus.LIVE);
        } else {
            application.setStatus(RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT);
        }

        restaurantOnboardingApplicationRepository.save(application);
        return application;
    }

    @Transactional
    public RestaurantOnboardingApplication rejectApplication(UUID applicationId, User admin,
                                                             RejectApplicationRequest request) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findByPublicId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (application.getStatus() != RestaurantOnboardingStatus.UNDER_REVIEW) {
            throw new InvalidApplicationStateException(
                    "Only applications under review can be rejected. Current status: " + application.getStatus());
        }

        application.setStatus(RestaurantOnboardingStatus.REJECTED);
        application.setReviewedByAdmin(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(request.getRejectionReason());

        restaurantOnboardingApplicationRepository.save(application);
        return application;
    }

    @Transactional
    public OrderResult createPaymentOrder(UUID applicationId, User user) throws RazorpayException {
        if (!isOwnerAccount(user)) {
            throw new AccessDeniedException("Only restaurant owners can pay the onboarding fee.");
        }

        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findByPublicId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        // Make sure the application belongs to this user
        if (!application.getUser().getId().equals(user.getId())) {
            throw new InvalidApplicationStateException("This application does not belong to you.");
        }

        // The onboarding fee is paid once per owner, not once per restaurant. If they've already
        // paid, every later application is approved straight to LIVE (see approveApplication) and
        // never reaches APPROVED_PENDING_PAYMENT, so this should never legitimately be called — but
        // guard against a stale client / double-click anyway.
        if (user.getOnboardingFeePaidAt() != null) {
            throw new OnboardingFeeAlreadyPaidException(
                    "You've already paid the one-time onboarding fee — no payment needed for this restaurant.");
        }

        // Only approved applications can pay
        if (application.getStatus() != RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT) {
            throw new InvalidApplicationStateException(
                    "Payment can only be initiated for approved applications. Current status: "
                            + application.getStatus());
        }

        // Reuse existing unpaid order
        Optional<OnboardingPayment> existingPayment =
                paymentRepository.findByRestaurantOnboardingApplication_Id(application.getId());

        if (existingPayment.isPresent()) {
            OnboardingPayment payment = existingPayment.get();
            if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.CREATED) {
                return new OrderResult(payment.getOrderId(), payment.getAmount().longValue());
            }
            if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
                throw new InvalidApplicationStateException("Payment has already been completed.");
            }
        }

        OrderResult orderResult = paymentService.createOrder(application.getId(), onboardingFeeAmount);

        OnboardingPayment payment = new OnboardingPayment();
        payment.setRestaurantOnboardingApplication(application);
        payment.setOrderId(orderResult.orderId());
        payment.setAmount((double) orderResult.amount());
        payment.setOnboardingPaymentStatus(OnboardingPaymentStatus.CREATED);

        paymentRepository.save(payment);
        return orderResult;
    }

    @Transactional
    public Restaurant verifyAndConfirmPayment(User user, String orderId, String paymentId,
                                              String razorpaySignature) {
        boolean valid = paymentService.verifyPayment(orderId, paymentId, razorpaySignature);
        if (!valid) {
            throw new InvalidApplicationStateException("Payment verification failed.");
        }

        OnboardingPayment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));

        RestaurantOnboardingApplication application = payment.getRestaurantOnboardingApplication();

        // Security: payment must belong to the logged-in user
        if (!application.getUser().getId().equals(user.getId())) {
            throw new InvalidApplicationStateException("This payment does not belong to you.");
        }

        // Payment must be for an approved application
        if (application.getStatus() != RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT) {
            if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
                throw new InvalidApplicationStateException("Payment has already been completed.");
            }
            throw new InvalidApplicationStateException(
                    "Payment is not allowed for the current application state: " + application.getStatus());
        }

        // Prevent duplicate payment processing — also what makes the entitlement grant below
        // idempotent against Razorpay webhook retries or a double-submitted client confirmation.
        if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
            throw new InvalidApplicationStateException("Payment has already been completed.");
        }

        // Payment is cryptographically verified; store Razorpay's payment ID.
        payment.setOnboardingPaymentStatus(OnboardingPaymentStatus.PAID);
        payment.setPaymentReference(paymentId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // First payment ever for this owner unlocks the restaurant quota. Guarded by the
        // PAID-status check just above, so this can only run once per owner.
        User owner = application.getUser();
        if (owner.getOnboardingFeePaidAt() == null) {
            owner.setOnboardingFeePaidAt(LocalDateTime.now());
            owner.setRestaurantQuota(RESTAURANT_QUOTA_ON_PAYMENT);
            userRepository.save(owner);
        }

        Restaurant savedRestaurant = createRestaurantFromApplication(application);

        application.setStatus(RestaurantOnboardingStatus.LIVE);
        restaurantOnboardingApplicationRepository.save(application);

        return savedRestaurant;
    }

    /** Builds and saves the Restaurant (+ address, bank details, timings) from an approved application. */
    private Restaurant createRestaurantFromApplication(RestaurantOnboardingApplication application) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUser(application.getUser());
        restaurant.setName(application.getRestaurantName());
        restaurant.setOwnerName(application.getOwnerName());
        restaurant.setFssaiLicense(application.getFssaiLicense());
        restaurant.setGstin(application.getGstin());
        restaurant.setIsOpen(true);
        restaurant.setRestaurantOnboardingStatus(RestaurantOnboardingStatus.LIVE);

        // Address: set both sides; the cascade on Restaurant.addresses saves it
        RestaurantAddress address = new RestaurantAddress();
        address.setStreet(application.getAddressLine());
        address.setCity(application.getCity());
        address.setState(application.getState());
        address.setCountry("India");
        address.setPincode(application.getPincode());
        address.setIsDefault(true);
        address.setRestaurant(restaurant);
        restaurant.getAddresses().add(address);

        BankDetails bankDetails = new BankDetails();
        bankDetails.setAccountHolderName(application.getAccountHolderName());
        bankDetails.setAccountNumber(application.getAccountNumber());
        bankDetails.setBankName(application.getBankName());
        bankDetails.setIfscCode(application.getIfscCode());
        bankDetails.setRestaurant(restaurant);
        restaurant.setBankDetails(bankDetails);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        List<RestaurantTimings> timings = application.getTimings().stream()
                .map(slot -> toRestaurantTimings(slot, savedRestaurant))
                .toList();
        restaurantTimingsRepository.saveAll(timings);

        return savedRestaurant;
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryResponse> listApplications(RestaurantOnboardingStatus status) {
        List<RestaurantOnboardingApplication> applications = (status != null)
                ? restaurantOnboardingApplicationRepository.findByStatus(status)
                : restaurantOnboardingApplicationRepository.findAllByOrderByIdDesc();
        return applications.stream().map(this::toSummary).toList();
    }

    /** Drives the "Add restaurant" UI: paid / quota / how many slots are already used. */
    @Transactional(readOnly = true)
    public OwnerEntitlementResponse getEntitlement(User user) {
        long used = restaurantOnboardingApplicationRepository
                .countByUser_IdAndStatusIn(user.getId(), QUOTA_OCCUPYING_STATUSES);
        return new OwnerEntitlementResponse(user.getOnboardingFeePaidAt() != null, user.getRestaurantQuota(), used);
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationSummaryResponse> getMyApplication(User user) {
        return restaurantOnboardingApplicationRepository
                .findFirstByUser_IdOrderByAttemptNumberDesc(user.getId())
                .map(this::toSummary);
    }

    private ApplicationSummaryResponse toSummary(RestaurantOnboardingApplication app) {
        return new ApplicationSummaryResponse(
                app.getPublicId(),
                app.getRestaurantName(),
                app.getUser().getEmail(),
                app.getCity(),
                app.getState(),
                app.getStatus(),
                app.getAttemptNumber(),
                app.getOwnerName());
    }

    private TimingSlotEmbeddable toEmbeddable(TimingSlotRequest slot) {
        TimingSlotEmbeddable embeddable = new TimingSlotEmbeddable();
        embeddable.setDayOfWeek(slot.getDayOfWeek());
        embeddable.setOpenTime(slot.getOpenTime());
        embeddable.setCloseTime(slot.getCloseTime());
        return embeddable;
    }

    private RestaurantTimings toRestaurantTimings(TimingSlotEmbeddable slot, Restaurant restaurant) {
        RestaurantTimings timing = new RestaurantTimings();
        timing.setRestaurant(restaurant);
        timing.setDayOfWeek(slot.getDayOfWeek());
        timing.setOpenTime(slot.getOpenTime());
        timing.setCloseTime(slot.getCloseTime());
        return timing;
    }
}