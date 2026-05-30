package com.nexaris.orgservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "org_user_roles",
        uniqueConstraints = @UniqueConstraint(name = "uq_org_user_roles", columnNames = {"user_id", "role_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrgUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
