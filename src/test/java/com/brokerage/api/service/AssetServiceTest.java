package com.brokerage.api.service;
import com.brokerage.api.dto.request.AssetFilterRequest;
import com.brokerage.api.dto.response.AssetResponse;
import com.brokerage.api.exception.ResourceNotFoundException;
import com.brokerage.api.model.Asset;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetServiceImpl assetService;

    private Long customerId;
    private Asset tryAsset;
    private Asset stockAsset;

    @BeforeEach
    void setUp() {
        customerId = 1L;

        // TRY asset
        tryAsset = Asset.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("TRY")
                .size(10000.0)
                .usableSize(10000.0)
                .build();

        // stock asset
        stockAsset = Asset.builder()
                .id(2L)
                .customerId(customerId)
                .assetName("AAPL")
                .size(100.0)
                .usableSize(100.0)
                .build();
    }

    @Test
    void getAssetsByCustomerId_Success() {
        // Given
        List<Asset> assets = Arrays.asList(tryAsset, stockAsset);
        when(assetRepository.findByCustomerId(customerId)).thenReturn(assets);

        // When
        List<AssetResponse> responses = assetService.getAssetsByCustomerId(customerId);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(a -> a.getAssetName().equals("TRY")));
        assertTrue(responses.stream().anyMatch(a -> a.getAssetName().equals("AAPL")));
    }

    @Test
    void getAssetByCustomerIdAndName_Success() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(tryAsset));

        // When
        AssetResponse response = assetService.getAssetByCustomerIdAndName(customerId, "TRY");

        // Then
        assertNotNull(response);
        assertEquals("TRY", response.getAssetName());
        assertEquals(10000.0, response.getSize());
        assertEquals(10000.0, response.getUsableSize());
    }

    @Test
    void getAssetByCustomerIdAndName_NotFound() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                assetService.getAssetByCustomerIdAndName(customerId, "INVALID"));
    }

    @Test
    void filterAssets_ByAssetName_Success() {
        // Given
        List<Asset> assets = Arrays.asList(tryAsset, stockAsset);
        when(assetRepository.findByCustomerId(customerId)).thenReturn(assets);

        AssetFilterRequest request = AssetFilterRequest.builder()
                .customerId(customerId)
                .assetName("TRY")
                .build();

        // When
        List<AssetResponse> responses = assetService.filterAssets(request);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("TRY", responses.get(0).getAssetName());
    }

    @Test
    void filterAssets_ShowOnlyPositive_Success() {
        // Given
        Asset emptyAsset = Asset.builder()
                .id(3L)
                .customerId(customerId)
                .assetName("EMPTY")
                .size(0.0)
                .usableSize(0.0)
                .build();

        List<Asset> assets = Arrays.asList(tryAsset, stockAsset, emptyAsset);
        when(assetRepository.findByCustomerId(customerId)).thenReturn(assets);

        AssetFilterRequest request = AssetFilterRequest.builder()
                .customerId(customerId)
                .showOnlyPositive(true)
                .build();

        // When
        List<AssetResponse> responses = assetService.filterAssets(request);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.stream().noneMatch(a -> a.getAssetName().equals("EMPTY")));
    }

    @Test
    void initializeCustomerAssets_NewCustomer_Success() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(tryAsset);

        // When
        assetService.initializeCustomerAssets(customerId, 10000.0);

        // Then
        verify(assetRepository).save(argThat(asset ->
                asset.getCustomerId().equals(customerId) &&
                        asset.getAssetName().equals("TRY") &&
                        asset.getSize() == 10000.0 &&
                        asset.getUsableSize() == 10000.0));
    }

    @Test
    void initializeCustomerAssets_ExistingCustomer_NoAction() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(tryAsset));

        // When
        assetService.initializeCustomerAssets(customerId, 10000.0);

        // Then
        verify(assetRepository, never()).save(any(Asset.class));
    }
}
