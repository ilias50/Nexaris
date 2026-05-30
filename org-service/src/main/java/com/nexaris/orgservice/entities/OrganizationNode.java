package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "organization_nodes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_organization_nodes_path", columnNames = "path"),
                @UniqueConstraint(name = "uq_organization_nodes_parent_slug", columnNames = {"parent_id", "slug"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_organization_nodes_parent"))
    private OrganizationNode parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<OrganizationNode> children = new ArrayList<>();

    @Column(name = "node_type", nullable = false, length = 50)
    private String nodeType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false, length = 1024)
    private String path;

    @Column(nullable = false)
    private Integer depth = 0;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private NodeContent content;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NodeLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NodeMembership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NodeAccessRule> accessRules = new ArrayList<>();

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private List<Announcement> announcements = new ArrayList<>();
}
