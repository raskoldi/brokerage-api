package com.brokerage.api.service.impl;

import com.brokerage.api.dto.request.AssetFilterRequest;
import com.brokerage.api.dto.response.AssetResponse;
import com.brokerage.api.exception.ResourceNotFoundException;
import com.brokerage.api.model.Asset;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    @Override
    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        log.debug("Getting assets for customer ID: {}", customerId);
        return assetRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetResponse> filterAssets(AssetFilterRequest request) {
        log.debug("Filtering assets with request: {}", request);
        List<Asset> assets = assetRepository.findByCustomerId(request.getCustomerId());

        // Apply filters
        if (request.getAssetName() != null && !request.getAssetName().isEmpty()) {
            assets = assets.stream()
                    .filter(asset -> asset.getAssetName().equalsIgnoreCase(request.getAssetName()))
                    .collect(Collectors.toList());
        }

        if (request.getShowOnlyPositive() != null && request.getShowOnlyPositive()) {
            assets = assets.stream()
                    .filter(asset -> asset.getSize() > 0)
                    .collect(Collectors.toList());
        }

        return assets.stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AssetResponse getAssetByCustomerIdAndName(Long customerId, String assetName) {
        log.debug("Getting asset for customer ID: {} and asset name: {}", customerId, assetName);
        Asset asset = assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .orElseThrow(() -> {
                    log.error("Asset not found for customer ID: {} and asset name: {}", customerId, assetName);
                    return new ResourceNotFoundException("Asset not found");
                });

        return mapToAssetResponse(asset);
    }

    @Override
    public AssetResponse getAssetById(Long assetId) {
        log.debug("Getting asset by ID: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset not found with ID: {}", assetId);
                    return new ResourceNotFoundException("Asset not found with ID: " + assetId);
                });

        return mapToAssetResponse(asset);
    }

    @Override
    @Transactional
    public void initializeCustomerAssets(Long customerId, Double initialTRYAmount) {
        log.debug("Initializing assets for customer ID: {} with TRY amount: {}", customerId, initialTRYAmount);
        // if customer already has TRY asset
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")
                .orElse(null);

        if (tryAsset == null) {
            // Creating new TRY asset for the customer
            tryAsset = Asset.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .size(initialTRYAmount)
                    .usableSize(initialTRYAmount)
                    .build();

            assetRepository.save(tryAsset);
            log.info("Created TRY asset for customer ID: {}", customerId);
        }
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .customerId(asset.getCustomerId())
                .assetName(asset.getAssetName())
                .size(asset.getSize())
                .usableSize(asset.getUsableSize())
                .build();
    }
}