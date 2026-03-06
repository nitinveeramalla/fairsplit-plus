package com.fairsplit.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_members")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@IdClass(GroupMemberId.class)
public class GroupMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.MEMBER;

    @Column(name = "joined_at")
    @Builder.Default
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    public enum Role { ADMIN, MEMBER }
}