package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "node_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false, foreignKey = @ForeignKey(name = "fk_node_links_node"))
    private OrganizationNode node;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 50)
    private String category = "GENERAL";

    @Column(length = 50)
    private String icon;

    @Column(nullable = false, length = 30)
    private String visibility = "INHERIT";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
