package com.brokerage.api.config;

import com.brokerage.api.model.Asset;
import com.brokerage.api.model.Customer;
import com.brokerage.api.model.User;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.repository.CustomerRepository;
import com.brokerage.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            initAdmin();

            // Creating first customer
            User customer1User = initCustomerUser("customer1", "password123");
            Customer customer1 = initCustomer("Customer 1", customer1User);
            initCustomerAssets(customer1.getId());

            // Creating second customer
            User customer2User = initCustomerUser("customer2", "password123");
            Customer customer2 = initCustomer("Customer 2", customer2User);
            initCustomerAssets(customer2.getId());

            logDatabaseState();
        };
    }

    @Transactional
    public User initAdmin() {
        log.info("Initializing admin user...");

        if (!userRepository.existsByUsername("admin")) {
            log.info("Creating admin user");
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Arrays.asList("ROLE_ADMIN"))
                    .build();

            admin = userRepository.save(admin);
            log.info("Admin user created with id: {}", admin.getId());
            return admin;
        } else {
            log.info("Admin user already exists");
            User admin = userRepository.findByUsername("admin").orElseThrow();
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin = userRepository.save(admin);
            log.info("Admin password updated");
            return admin;
        }
    }

    @Transactional
    public User initCustomerUser(String username, String password) {
        log.info("Initializing customer user: {}...", username);

        if (!userRepository.existsByUsername(username)) {
            log.info("Creating customer user: {}", username);
            User customer = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .roles(Arrays.asList("ROLE_CUSTOMER"))
                    .build();

            customer = userRepository.save(customer);
            log.info("Customer user created with id: {}", customer.getId());
            return customer;
        } else {
            log.info("Customer user already exists: {}", username);
            User customer = userRepository.findByUsername(username).orElseThrow();
            customer.setPassword(passwordEncoder.encode(password));
            customer = userRepository.save(customer);
            return customer;
        }
    }

    @Transactional
    public Customer initCustomer(String customerName, User user) {
        log.info("Initializing customer: {}...", customerName);

        Optional<Customer> existingCustomer = customerRepository.findByUser(user);

        if (existingCustomer.isEmpty()) {
            log.info("Creating customer: {} associated with user ID: {}", customerName, user.getId());
            Customer customer = Customer.builder()
                    .customerName(customerName)
                    .user(user)
                    .build();

            customer = customerRepository.save(customer);
            log.info("Customer created with id: {}", customer.getId());
            return customer;
        } else {
            Customer customer = existingCustomer.get();
            log.info("Customer already exists with id: {} for user ID: {}",
                    customer.getId(), user.getId());
            return customer;
        }
    }

    @Transactional
    public void initCustomerAssets(Long customerId) {
        log.info("Initializing assets for customer ID: {}", customerId);

        //  Assets
        initAsset(customerId, "TRY", 10000.0);
        initAsset(customerId, "AAPL", 100.0);
        initAsset(customerId, "GOOGL", 50.0);
        initAsset(customerId, "MSFT", 75.0);

        log.info("Assets initialization completed for customer ID: {}", customerId);
    }

    private void initAsset(Long customerId, String assetName, Double initialAmount) {
        if (assetRepository.findByCustomerIdAndAssetName(customerId, assetName).isEmpty()) {
            log.info("Creating asset {} for customer ID {}", assetName, customerId);
            Asset asset = Asset.builder()
                    .customerId(customerId)
                    .assetName(assetName)
                    .size(initialAmount)
                    .usableSize(initialAmount)
                    .build();

            assetRepository.save(asset);
            log.info("Asset created: {}", assetName);
        } else {
            log.info("Asset {} already exists for customer ID {}", assetName, customerId);
        }
    }

    private void logDatabaseState() {
        log.info("============= DATABASE STATE =============");
        // Log all users
        log.info("USERS:");
        userRepository.findAll().forEach(user ->
                log.info("  ID: {}, Username: {}, Roles: {}",
                        user.getId(), user.getUsername(), user.getRoles()));

        // Log all customers
        log.info("CUSTOMERS:");
        customerRepository.findAll().forEach(customer ->
                log.info("  ID: {}, Name: {}, User ID: {}",
                        customer.getId(), customer.getCustomerName(),
                        customer.getUser() != null ? customer.getUser().getId() : "null"));

        // Log all assets
        log.info("ASSETS:");
        assetRepository.findAll().forEach(asset ->
                log.info("  ID: {}, Customer ID: {}, Asset: {}, Size: {}, Usable Size: {}",
                        asset.getId(), asset.getCustomerId(), asset.getAssetName(),
                        asset.getSize(), asset.getUsableSize()));
        log.info("=========================================");
    }
}