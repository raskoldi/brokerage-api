package com.brokerage.api.service;
import com.brokerage.api.dto.request.AssetFilterRequest;
import com.brokerage.api.dto.response.AssetResponse;
import java.util.List;

public interface AssetService {

    List<AssetResponse> getAssetsByCustomerId(Long customerId);

    List<AssetResponse> filterAssets(AssetFilterRequest request);

    AssetResponse getAssetByCustomerIdAndName(Long customerId, String assetName);

    // New method to get asset by ID
    AssetResponse getAssetById(Long assetId);

    // Method to initialize a new customer with TRY asset
    void initializeCustomerAssets(Long customerId, Double initialTRYAmount);
}