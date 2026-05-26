package com.rallycourt.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends AbstractEntity {

    private String email;
    private String firstName;
    private String lastName;
    private String mobileNumber;

    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private CourtOwnerStatus courtOwnerStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
