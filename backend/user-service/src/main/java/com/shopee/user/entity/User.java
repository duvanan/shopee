package com.shopee.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entity phang - khong co @ManyToMany toi Role. Role cua user duoc doc/ghi
 * qua native query rieng tren bang user_roles (xem RoleRepository), khong
 * qua object graph cua JPA. Cot audit (created_at/updated_at/created_by/
 * updated_by) ke thua tu Auditable, Spring Data JPA Auditing tu gan gia tri.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String phone;
}
