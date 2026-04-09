package com.hubilon.modules.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class User {

    private Long id;
    private String name;
    private String email;
    private String password;
    private String department;
    private Role role;

    public enum Role {
        ADMIN, USER
    }
}
