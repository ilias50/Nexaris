package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "org_catalog_entries",
    uniqueConstraints = @UniqueConstraint(name = "uq_catalog_type_value", columnNames = {"catalog_type", "value"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrgCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "catalog_type", nullable = false, length = 60)
    private String catalogType;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(length = 20)
    private String color;
}
