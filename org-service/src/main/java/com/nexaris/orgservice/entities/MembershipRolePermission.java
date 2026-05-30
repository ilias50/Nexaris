package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "membership_role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_membership_role_permission",
                columnNames = {"membership_role", "permission"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembershipRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "membership_role", nullable = false, length = 50)
    private String membershipRole;

    @Column(name = "permission", nullable = false, length = 60)
    private String permission;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
