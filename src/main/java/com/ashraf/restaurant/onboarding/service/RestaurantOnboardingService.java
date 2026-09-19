package com.ashraf.restaurant.onboarding.service;

import com.ashraf.commerce.entity.BankDetails;
import com.ashraf.core.entity.User;
import com.ashraf.payment.OrderResult;
import com.ashraf.payment.dto.OnboardingPayment;
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
import com.ashraf.restaurant.onboarding.dto.RejectApplicationRequest;
import com.ashraf.restaurant.onboarding.dto.RestaurantOnboardingApplicationRequest;
import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import com.ashraf.restaurant.onboarding.entity.TimingSlotEmbeddable;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.restaurant.onboarding.repository.RestaurantOnboardingApplicationRepository;
import com.ashraf.shared.exception.ApplicationAlreadyActiveException;
import com.ashraf.shared.exception.ApplicationNotFoundException;
import com.ashraf.shared.exception.InvalidApplicationStateException;
import com.ashraf.shared.exception.PaymentNotFoundException;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RestaurantOnboardingService {

    private final RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTimingsRepository restaurantTimingsRepository;
    private final OnboardingPaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Value("${onboarding.fee.amount}")
    private double onboardingFeeAmount;

    public RestaurantOnboardingService(
            RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository,
            RestaurantRepository restaurantRepository,
            RestaurantTimingsRepository restaurantTimingsRepository,
            OnboardingPaymentRepository paymentRepository,
            PaymentService paymentService) {
        this.restaurantOnboardingApplicationRepository = restaurantOnboardingApplicationRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantTimingsRepository = restaurantTimingsRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public void submitApplication(User user, RestaurantOnboardingApplicationRequest request) {
        boolean hasActiveApplication = restaurantOnboardingApplicationRepository
                .existsByUser_IdAndStatusIn(user.getId(), List.of(
                        RestaurantOnboardingStatus.UNDER_REVIEW,
                        RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT));

        if (hasActiveApplication) {
            throw new ApplicationAlreadyActiveException(
                    "You already have an application under review or awaiting payment.");
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

    @Transactional
    public RestaurantOnboardingApplication approveApplication(Long applicationId, User admin) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (application.getStatus() != RestaurantOnboardingStatus.UNDER_REVIEW) {
            throw new InvalidApplicationStateException(
                    "Only applications under review can be approved. Current status: " + application.getStatus());
        }

        application.setStatus(RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT);
        application.setReviewedByAdmin(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(null);

        restaurantOnboardingApplicationRepository.save(application);
        return application;
    }

    @Transactional
    public RestaurantOnboardingApplication rejectApplication(Long applicationId, User admin,
                                                             RejectApplicationRequest request) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findById(applicationId)
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
    public OrderResult createPaymentOrder(Long applicationId, User user) throws RazorpayException {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        // Make sure the application belongs to this user
        if (!application.getUser().getId().equals(user.getId())) {
            throw new InvalidApplicationStateException("This application does not belong to you.");
        }

        // Only approved applications can pay
        if (application.getStatus() != RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT) {
            throw new InvalidApplicationStateException(
                    "Payment can only be initiated for approved applications. Current status: "
                            + application.getStatus());
        }

        // Reuse existing unpaid order
        Optional<OnboardingPayment> existingPayment =
                paymentRepository.findByRestaurantOnboardingApplication_Id(applicationId);

        if (existingPayment.isPresent()) {
            OnboardingPayment payment = existingPayment.get();
            if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.CREATED) {
                return new OrderResult(payment.getOrderId(), payment.getAmount().longValue());
            }
            if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
                throw new InvalidApplicationStateException("Payment has already been completed.");
            }
        }

        OrderResult orderResult = paymentService.createOrder(applicationId, onboardingFeeAmount);

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

        // Prevent duplicate payment processing
        if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
            throw new InvalidApplicationStateException("Payment has already been completed.");
        }

        // Payment is cryptographically verified; store Razorpay's payment ID.
        payment.setOnboardingPaymentStatus(OnboardingPaymentStatus.PAID);
        payment.setPaymentReference(paymentId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Create restaurant
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

        // Application is now fully onboarded
        application.setStatus(RestaurantOnboardingStatus.LIVE);
        restaurantOnboardingApplicationRepository.save(application);

        return savedRestaurant;
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryResponse> listApplications(RestaurantOnboardingStatus status) {
        List<RestaurantOnboardingApplication> applications = (status != null)
                ? restaurantOnboardingApplicationRepository.findByStatus(status)
                : restaurantOnboardingApplicationRepository.findAllByOrderByIdDesc();
        return applications.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationSummaryResponse> getMyApplication(User user) {
        return restaurantOnboardingApplicationRepository
                .findFirstByUser_IdOrderByAttemptNumberDesc(user.getId())
                .map(this::toSummary);
    }

    private ApplicationSummaryResponse toSummary(RestaurantOnboardingApplication app) {
        return new ApplicationSummaryResponse(
                app.getId(),
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