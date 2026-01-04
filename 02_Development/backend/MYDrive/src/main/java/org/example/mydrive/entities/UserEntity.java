package org.example.mydrive.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Stored hashed

    @Column(unique = true, nullable = false)
    private String email;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @Column(nullable = false)
    private Long currentStorageUsed; // In bytes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferredLanguageId") // FK to LANGUAGES.id
    private LanguageEntity preferredLanguageEntity;

     @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
     private UserSubscriptionEntity activeSubscription;
}
