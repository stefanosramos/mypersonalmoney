package br.com.mypersonalmoney.security;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
@UserDefinition
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Username
    @Column(nullable = false, unique = true)
    public String username;

    @Password
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Roles
    @Column(nullable = false)
    public String role;

    @Column(nullable = false)
    public boolean active = true;
}