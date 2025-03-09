package com.brokerage.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assets", uniqueConstraints = {@UniqueConstraint(columnNames = {"customerId", "assetName"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String assetName;

    @Column(nullable = false)
    private Double size;

    @Column(nullable = false)
    private Double usableSize;

}