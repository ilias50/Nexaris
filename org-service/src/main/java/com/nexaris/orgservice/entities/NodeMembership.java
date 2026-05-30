package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "node_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_node_memberships_assignment",
                columnNames = {"node_id", "user_id", "membership_role"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false, foreignKey = @ForeignKey(name = "fk_node_memberships_node"))
    private OrganizationNode node;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "membership_role", nullable = false, length = 50)
        private String membershipRole = "MEMBRE";

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "active_from")
    private LocalDateTime activeFrom;

    @Column(name = "active_to")
    private LocalDateTime activeTo;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
