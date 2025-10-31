package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // One user has one active subscription
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity planEntity;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate; // Nullable for active subscriptions

    @Column(nullable = false, length = 50)
    private String status; // e.g., "ACTIVE", "EXPIRED", "CANCELLED"

    private LocalDateTime renewalDate;

    private String paymentMethodId; // Gateway's payment method ID

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
