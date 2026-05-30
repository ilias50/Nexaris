package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "node_access_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_node_access_rules",
                columnNames = {"node_id", "effect", "subject_type", "subject_value", "permission"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeAccessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false, foreignKey = @ForeignKey(name = "fk_node_access_rules_node"))
    private OrganizationNode node;

    @Column(nullable = false, length = 10)
    private String effect = "ALLOW";

    @Column(name = "subject_type", nullable = false, length = 30)
    private String subjectType;

    @Column(name = "subject_value", nullable = false, length = 100)
    private String subjectValue;

    @Column(nullable = false, length = 30)
    private String permission = "READ";

    @Column(name = "applies_to_children", nullable = false)
    private boolean appliesToChildren = false;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
