package com.ashraf.restaurant.onboarding.service;

import com.ashraf.commerce.entity.BankDetails;
import com.ashraf.core.entity.User;
import com.ashraf.payment.OrderResult;
import com.ashraf.payment.dto.OnboardingPayment;
import com.ashraf.payment.enums.OnboardingPaymentStatus;
import com.ashraf.payment.repository.OnboardingPaymentRepository;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.entity.RestaurantAddress;
import com.ashraf.restaurant.core.repository.RestaurantAddressRepository;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.onboarding.dto.ApplicationSummaryResponse;
import com.ashraf.restaurant.onboarding.dto.ConfirmPaymentRequest;
import com.ashraf.restaurant.onboarding.dto.RejectApplicationRequest;
import com.ashraf.restaurant.onboarding.dto.RestaurantOnboardingApplicationRequest;
import com.ashraf.restaurant.onboarding.entity.RestaurantOnboardingApplication;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.restaurant.onboarding.repository.RestaurantOnboardingApplicationRepository;
import com.ashraf.shared.exception.ApplicationAlreadyActiveException;
import com.ashraf.shared.exception.ApplicationNotFoundException;
import com.ashraf.shared.exception.InvalidApplicationStateException;
import com.ashraf.shared.exception.PaymentNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RestaurantOnboardingService {
    private final RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantAddressRepository restaurantAddressRepository;
    private final OnboardingPaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;


    @Value("${onboarding.fee.amount}")
    private Long onboardingFeeAmount;

    public RestaurantOnboardingService(RestaurantOnboardingApplicationRepository restaurantOnboardingApplicationRepository, RestaurantRepository restaurantRepository, RestaurantAddressRepository restaurantAddressRepository, OnboardingPaymentRepository paymentRepository, PaymentGatewayService paymentGatewayService) {
        this.restaurantOnboardingApplicationRepository = restaurantOnboardingApplicationRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantAddressRepository = restaurantAddressRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    @Transactional
    public void submitApplication(User user, RestaurantOnboardingApplicationRequest request) {
        boolean hasActiveApplication  = restaurantOnboardingApplicationRepository.findByUser_IdAndStatusIn(user.getId(), List.of(
                RestaurantOnboardingStatus.UNDER_REVIEW,RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT
        )).isPresent();

        if (hasActiveApplication) {
            throw new ApplicationAlreadyActiveException(
                    "You already have an application under review or awaiting payment.");
        }

        int nextAttemptNumber = restaurantOnboardingApplicationRepository
                .findByUser_IdOrderByAttemptNumberDesc(user.getId())
                .stream()
                .findFirst()
                .map(app -> app.getAttemptNumber() + 1)
                .orElse(1);

        RestaurantOnboardingApplication application = new RestaurantOnboardingApplication();
        application.setUser(user);
        application.setAttemptNumber(nextAttemptNumber);
        application.setRestaurantName(request.getRestaurantName());
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
        application.setStatus(RestaurantOnboardingStatus.UNDER_REVIEW);

        restaurantOnboardingApplicationRepository.save(application);
    }

    @Transactional
    public RestaurantOnboardingApplication approveApplication(Long applicationId, User admin) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository.findById(applicationId)
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
    public RestaurantOnboardingApplication rejectApplication(Long applicationId, User admin, RejectApplicationRequest request) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository.findById(applicationId)
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
    public OrderResult createPaymentOrder(Long applicationId, User user) {
        RestaurantOnboardingApplication application = restaurantOnboardingApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new InvalidApplicationStateException("This application does not belong to you.");
        }

        if (application.getStatus() != RestaurantOnboardingStatus.APPROVED_PENDING_PAYMENT) {
            throw new InvalidApplicationStateException(
                    "Payment can only be initiated for approved applications. Current status: " + application.getStatus());
        }

        // reuse an existing CREATED payment instead of inserting a duplicate
        Optional<OnboardingPayment> existing = paymentRepository.findByRestaurantOnboardingApplication_Id(applicationId);
        if (existing.isPresent() && existing.get().getOnboardingPaymentStatus() == OnboardingPaymentStatus.CREATED) {
            OnboardingPayment payment = existing.get();
            return new OrderResult(payment.getOrderId(), payment.getAmount());
        }

        OrderResult orderResult = paymentGatewayService.createOrder(applicationId, onboardingFeeAmount);

        OnboardingPayment payment = new OnboardingPayment();
        payment.setRestaurantOnboardingApplication(application);
        payment.setOrderId(orderResult.orderId());
        payment.setAmount(orderResult.amount());
        payment.setOnboardingPaymentStatus(OnboardingPaymentStatus.CREATED);

        paymentRepository.save(payment);

        return orderResult;
    }

    @Transactional
    public Restaurant confirmPayment(ConfirmPaymentRequest request) {

        OnboardingPayment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + request.getOrderId()));

        if (payment.getOnboardingPaymentStatus() == OnboardingPaymentStatus.PAID) {
            throw new InvalidApplicationStateException("Payment already confirmed for this order.");
        }

        payment.setOnboardingPaymentStatus(OnboardingPaymentStatus.PAID);
        payment.setPaymentReference(request.getPaymentReference());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        RestaurantOnboardingApplication application = payment.getRestaurantOnboardingApplication();

        RestaurantAddress address = new RestaurantAddress();
        address.setStreet(application.getAddressLine());
        address.setCity(application.getCity());
        address.setState(application.getState());
        address.setCountry("India");
        address.setPincode(application.getPincode());
        // Restaurant.restaurantAddress is a plain @ManyToOne with no cascade, and the
        // column is nullable = false, so the address must exist in the DB before a
        // Restaurant can reference it.
        address = restaurantAddressRepository.save(address);

        BankDetails bankDetails = new BankDetails();
        bankDetails.setAccountHolderName(application.getAccountHolderName());
        bankDetails.setAccountNumber(application.getAccountNumber());
        bankDetails.setBankName(application.getBankName());
        bankDetails.setIfscCode(application.getIfscCode());

        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUser(application.getUser());
        restaurant.setName(application.getRestaurantName());
        restaurant.setFssaiLicense(application.getFssaiLicense());
        restaurant.setGstin(application.getGstin());
        restaurant.setAddresses(List.of(address));
        restaurant.setBankDetails(bankDetails);
        // BankDetails owns the FK (restaurant_id, nullable = false). Restaurant's side
        // of the relationship is mappedBy, so cascading the save from Restaurant only
        // works if the owning side's back-reference is set here too.
        bankDetails.setRestaurant(restaurant);
        restaurant.setIsOpen(true);
        restaurant.setRestaurantOnboardingStatus(RestaurantOnboardingStatus.LIVE);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        application.setStatus(RestaurantOnboardingStatus.LIVE);
        restaurantOnboardingApplicationRepository.save(application);

        return savedRestaurant;
    }
    public List<ApplicationSummaryResponse> listApplications(RestaurantOnboardingStatus status) {
        List<RestaurantOnboardingApplication> applications = (status != null)
                ? restaurantOnboardingApplicationRepository.findByStatus(status)
                : restaurantOnboardingApplicationRepository.findAllByOrderByIdDesc();

        return applications.stream()
                .map(app -> new ApplicationSummaryResponse(
                        app.getId(),
                        app.getRestaurantName(),
                        app.getUser().getEmail(),
                        app.getCity(),
                        app.getState(),
                        app.getStatus(),
                        app.getAttemptNumber()
                ))
                .toList();
    }
    public Optional<ApplicationSummaryResponse> getMyApplication(User user) {
        return restaurantOnboardingApplicationRepository
                .findFirstByUser_IdOrderByAttemptNumberDesc(user.getId())
                .map(app -> new ApplicationSummaryResponse(
                        app.getId(),
                        app.getRestaurantName(),
                        app.getUser().getEmail(),
                        app.getCity(),
                        app.getState(),
                        app.getStatus(),
                        app.getAttemptNumber()
                ));
    }
}