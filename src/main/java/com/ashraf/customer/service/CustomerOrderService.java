package com.ashraf.customer.service;

import com.ashraf.commerce.enums.PaymentStatus;
import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.OrderLineRequest;
import com.ashraf.customer.dto.OrderQuoteResponse;
import com.ashraf.customer.dto.OrderResponse;
import com.ashraf.customer.dto.OrderSummaryResponse;
import com.ashraf.customer.dto.PlaceOrderRequest;
import com.ashraf.customer.dto.QuoteRequest;
import com.ashraf.customer.entity.Customer;
import com.ashraf.customer.entity.CustomerAddress;
import com.ashraf.restaurant.core.entity.Menu;
import com.ashraf.restaurant.core.entity.Order;
import com.ashraf.restaurant.core.entity.OrderItem;
import com.ashraf.restaurant.core.entity.Restaurant;
import com.ashraf.restaurant.core.enums.OrderStatus;
import com.ashraf.restaurant.core.repository.MenuRepository;
import com.ashraf.restaurant.core.repository.OrderRepository;
import com.ashraf.restaurant.core.repository.RestaurantRepository;
import com.ashraf.restaurant.onboarding.enums.RestaurantOnboardingStatus;
import com.ashraf.shared.dto.PageResponse;
import com.ashraf.shared.exception.BusinessRuleException;
import com.ashraf.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer-side order lifecycle: quote -> place -> track -> cancel.
 * Every price is read from the database here; nothing the client sends is trusted for money.
 */
@Service
public class CustomerOrderService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final CustomerLookupService customerLookup;
    private final CustomerAddressService addressService;
    private final OrderPricingService pricing;
    private final OrderPricingProperties props;

    public CustomerOrderService(OrderRepository orderRepository,
                                RestaurantRepository restaurantRepository,
                                MenuRepository menuRepository,
                                CustomerLookupService customerLookup,
                                CustomerAddressService addressService,
                                OrderPricingService pricing,
                                OrderPricingProperties props) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuRepository = menuRepository;
        this.customerLookup = customerLookup;
        this.addressService = addressService;
        this.pricing = pricing;
        this.props = props;
    }

    // ---- quote ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderQuoteResponse quote(QuoteRequest req) {
        ResolvedCart cart = resolveCart(req.restaurantId(), req.items());
        OrderPricingService.PriceBreakdown price = pricing.price(cart.subtotal());

        List<OrderQuoteResponse.Line> lines = cart.lines().stream()
                .map(l -> new OrderQuoteResponse.Line(
                        l.menu().getId(),
                        l.menu().getDishName(),
                        l.menu().getImageUrl(),
                        l.menu().getFoodType() != null ? l.menu().getFoodType().name() : null,
                        l.unitPrice(),
                        l.quantity(),
                        l.lineTotal()))
                .toList();

        return new OrderQuoteResponse(
                cart.restaurant().getId(),
                cart.restaurant().getName(),
                lines,
                price.subtotal(), price.deliveryFee(), price.taxAmount(),
                price.discountAmount(), price.totalAmount(),
                pricing.amountForFreeDelivery(price.subtotal()),
                props.getMinOrderAmount(),
                pricing.estimateDeliveryMinutes(cart.longestPrepMinutes()));
    }

    // ---- place ---------------------------------------------------------------------------

    @Transactional
    public OrderResponse placeOrder(User user, PlaceOrderRequest req) {
        if (!props.getAllowedPaymentMethods().contains(req.paymentMethod())) {
            throw new BusinessRuleException(
                    "Payment method " + req.paymentMethod() + " is not available right now.");
        }

        Customer customer = customerLookup.requireCustomer(user);
        CustomerAddress address = addressService.requireOwnedAddress(customer, req.addressId());
        ResolvedCart cart = resolveCart(req.restaurantId(), req.items());

        if (cart.subtotal().compareTo(props.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException(
                    "Minimum order amount is ₹" + props.getMinOrderAmount().stripTrailingZeros().toPlainString() + ".");
        }

        OrderPricingService.PriceBreakdown price = pricing.price(cart.subtotal());
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(cart.restaurant());
        order.setDeliveryAddress(address);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderDate(now);
        order.setSubtotal(price.subtotal());
        order.setDeliveryFee(price.deliveryFee());
        order.setTaxAmount(price.taxAmount());
        order.setDiscountAmount(price.discountAmount());
        order.setTotalAmount(price.totalAmount());
        order.setPaymentMethod(req.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSpecialInstructions(blankToNull(req.specialInstructions()));
        order.setEstimatedDeliveryTime(
                now.plusMinutes(pricing.estimateDeliveryMinutes(cart.longestPrepMinutes())));

        List<OrderItem> items = new ArrayList<>();
        for (ResolvedLine line : cart.lines()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenu(line.menu());
            item.setQuantity(line.quantity());
            item.setPriceAtOrder(line.unitPrice());
            item.setDishNameSnapshot(line.menu().getDishName());
            item.setSpecialInstructions(line.instructions());
            items.add(item);
        }
        order.setItems(items);

        return OrderResponse.from(orderRepository.save(order));
    }

    // ---- read / cancel -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(User user, int page, int size) {
        Customer customer = customerLookup.requireCustomer(user);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Page<Order> result = orderRepository.findByCustomer_IdOrderByCreatedAtDesc(
                customer.getId(), PageRequest.of(Math.max(page, 0), safeSize));
        return PageResponse.of(result, result.getContent().stream().map(OrderSummaryResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(User user, Long orderId) {
        return OrderResponse.from(requireOrder(user, orderId));
    }

    @Transactional
    public OrderResponse cancel(User user, Long orderId, String reason) {
        Order order = requireOrder(user, orderId);
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleException(
                    "This order is already being prepared and can no longer be cancelled.");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(blankToNull(reason) != null ? reason.trim() : "Cancelled by customer");
        return OrderResponse.from(order);
    }

    private Order requireOrder(User user, Long orderId) {
        Customer customer = customerLookup.requireCustomer(user);
        return orderRepository.findWithDetailsByIdAndCustomer_Id(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    // ---- cart resolution -----------------------------------------------------------------

    private record ResolvedLine(Menu menu, int quantity, String instructions,
                                BigDecimal unitPrice, BigDecimal lineTotal) {}

    private record ResolvedCart(Restaurant restaurant, List<ResolvedLine> lines,
                                BigDecimal subtotal, int longestPrepMinutes) {}

    /** Validates a cart against live data and prices it from the database. */
    private ResolvedCart resolveCart(Long restaurantId, List<OrderLineRequest> requested) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .filter(r -> r.getRestaurantOnboardingStatus() == RestaurantOnboardingStatus.LIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (!Boolean.TRUE.equals(restaurant.getIsOpen())) {
            throw new BusinessRuleException(
                    restaurant.getName() + " is closed right now and isn't taking orders.");
        }

        // Merge duplicate menu ids so the same dish can't appear on two lines.
        Map<Long, OrderLineRequest> merged = new LinkedHashMap<>();
        for (OrderLineRequest line : requested) {
            merged.merge(line.menuId(), line, (a, b) -> new OrderLineRequest(
                    a.menuId(), a.quantity() + b.quantity(),
                    a.specialInstructions() != null ? a.specialInstructions() : b.specialInstructions()));
        }

        Map<Long, Menu> menus = menuRepository.findAllById(merged.keySet()).stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        List<ResolvedLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int longestPrep = 0;

        for (OrderLineRequest line : merged.values()) {
            Menu menu = menus.get(line.menuId());
            if (menu == null || !menu.getRestaurant().getId().equals(restaurantId)) {
                throw new BusinessRuleException(
                        "Some items in your cart are no longer available. Please review your cart.");
            }
            if (line.quantity() > props.getMaxQuantityPerItem()) {
                throw new BusinessRuleException(
                        "You can order at most " + props.getMaxQuantityPerItem() + " of " + menu.getDishName() + ".");
            }
            BigDecimal lineTotal = menu.getPrice().multiply(BigDecimal.valueOf(line.quantity()));
            subtotal = subtotal.add(lineTotal);
            if (menu.getPreparationTimeMinutes() != null) {
                longestPrep = Math.max(longestPrep, menu.getPreparationTimeMinutes());
            }
            lines.add(new ResolvedLine(menu, line.quantity(), blankToNull(line.specialInstructions()),
                    menu.getPrice(), lineTotal));
        }
        return new ResolvedCart(restaurant, lines, subtotal, longestPrep);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
