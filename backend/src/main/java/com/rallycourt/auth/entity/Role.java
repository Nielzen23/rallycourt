package com.rallycourt.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = "role")
    private Set<User> users = new HashSet<>();
}
