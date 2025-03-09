package com.brokerage.api.security;

import com.brokerage.api.model.Asset;
import com.brokerage.api.model.Customer;
import com.brokerage.api.model.Order;
import com.brokerage.api.model.User;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.repository.CustomerRepository;
import com.brokerage.api.repository.OrderRepository;
import com.brokerage.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public boolean isCustomerOwner(Long customerId, Authentication authentication) {
        if (authentication == null) {
            log.debug("Authentication is null, access denied");
            return false;
        }

        // If admin, allow access
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.debug("User is admin, access granted for customer ID: {}", customerId);
            return true;
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            log.debug("User principal ID: {}, username: {}", userPrincipal.getId(), userPrincipal.getUsername());

            // the customer associated with the authenticated user
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> {
                        log.warn("User not found with ID: {}", userPrincipal.getId());
                        return new AccessDeniedException("User not found");
                    });

            // the customer(s) associated with this user
            Customer associatedCustomer = customerRepository.findByUser(user)
                    .orElseThrow(() -> {
                        log.warn("No customer associated with user: {}", user.getUsername());
                        return new AccessDeniedException("No customer profile found for this user");
                    });

            log.debug("User {} is associated with customer ID: {}",
                    user.getUsername(), associatedCustomer.getId());

            // if the requested customer ID matches the user's associated customer
            boolean isOwner = associatedCustomer.getId().equals(customerId);
            log.debug("Is user {} owner of requested customer {}? {}",
                    user.getUsername(), customerId, isOwner);

            if (!isOwner) {
                log.warn("Access denied: User {} (customer ID {}) is not authorized to access customer {}",
                        user.getUsername(), associatedCustomer.getId(), customerId);
                throw new AccessDeniedException("You do not have permission to access this customer's data");
            }

            return isOwner;
        } catch (ClassCastException e) {
            log.error("Failed to cast Authentication principal to UserPrincipal", e);
            return false;
        }
    }

    public boolean isOrderOwner(Long orderId, Authentication authentication) {
        if (authentication == null) {
            log.debug("Authentication is null, access denied");
            return false;
        }

        // If admin, allow access
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.debug("User is admin, access granted for order ID: {}", orderId);
            return true;
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // the user's associated customer
            User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
            Customer associatedCustomer = customerRepository.findByUser(user).orElseThrow();
            Long userCustomerId = associatedCustomer.getId();

            // if the order belongs to this customer
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with ID: {}", orderId);
                        return new AccessDeniedException("Order not found");
                    });

            boolean isOwner = order.getCustomerId().equals(userCustomerId);
            log.debug("Is customer {} owner of order {}? {}", userCustomerId, orderId, isOwner);

            if (!isOwner) {
                log.warn("Access denied: Customer {} is not authorized to access order {}",
                        userCustomerId, orderId);
                throw new AccessDeniedException("You do not have permission to access this order");
            }

            return isOwner;
        } catch (Exception e) {
            log.error("Error checking order ownership", e);
            return false;
        }
    }

    public boolean isAssetOwner(Long assetId, Authentication authentication) {
        if (authentication == null) {
            log.debug("Authentication is null, access denied");
            return false;
        }

        // If admin, allow access
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.debug("User is admin, access granted for asset ID: {}", assetId);
            return true;
        }

        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // the user's associated customer
            User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
            Customer associatedCustomer = customerRepository.findByUser(user).orElseThrow();
            Long userCustomerId = associatedCustomer.getId();

            // if the asset belongs to this customer
            Asset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> {
                        log.warn("Asset not found with ID: {}", assetId);
                        return new AccessDeniedException("Asset not found");
                    });

            boolean isOwner = asset.getCustomerId().equals(userCustomerId);
            log.debug("Is customer {} owner of asset {}? {}", userCustomerId, assetId, isOwner);

            if (!isOwner) {
                log.warn("Access denied: Customer {} is not authorized to access asset {}",
                        userCustomerId, assetId);
                throw new AccessDeniedException("You do not have permission to access this asset");
            }

            return isOwner;
        } catch (Exception e) {
            log.error("Error checking asset ownership", e);
            return false;
        }
    }
}