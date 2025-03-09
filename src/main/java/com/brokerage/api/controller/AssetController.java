package com.brokerage.api.controller;

import com.brokerage.api.dto.request.AssetFilterRequest;
import com.brokerage.api.dto.response.AssetResponse;
import com.brokerage.api.exception.ResourceNotFoundException;
import com.brokerage.api.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCustomerOwner(#customerId, authentication)")
    public ResponseEntity<List<AssetResponse>> getAssetsByCustomerId(@RequestParam Long customerId) {
        log.info("Getting assets for customer ID: {}", customerId);
        return ResponseEntity.ok(assetService.getAssetsByCustomerId(customerId));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCustomerOwner(#customerId, authentication)")
    public ResponseEntity<List<AssetResponse>> filterAssets(
            @RequestParam Long customerId,
            @RequestParam(required = false) String assetName,
            @RequestParam(required = false) Boolean showOnlyPositive) {

        log.info("Filtering assets for customer ID: {}, assetName: {}, showOnlyPositive: {}",
                customerId, assetName, showOnlyPositive);

        AssetFilterRequest request = AssetFilterRequest.builder()
                .customerId(customerId)
                .assetName(assetName)
                .showOnlyPositive(showOnlyPositive)
                .build();

        return ResponseEntity.ok(assetService.filterAssets(request));
    }

    @GetMapping("/{customerId}/{assetName}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCustomerOwner(#customerId, authentication)")
    public ResponseEntity<AssetResponse> getAssetByCustomerIdAndName(
            @PathVariable Long customerId,
            @PathVariable String assetName) {
        log.info("Getting asset for customer ID: {} and asset name: {}", customerId, assetName);
        return ResponseEntity.ok(assetService.getAssetByCustomerIdAndName(customerId, assetName));
    }

    @GetMapping("/id/{assetId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isAssetOwner(#assetId, authentication)")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable Long assetId) {
        log.info("Getting asset by ID: {}", assetId);
        try {
            return ResponseEntity.ok(assetService.getAssetById(assetId));
        } catch (ResourceNotFoundException e) {
            log.error("Asset not found with ID: {}", assetId);
            throw e;
        }
    }
}