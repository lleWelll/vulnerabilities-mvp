package org.aitu.vulnerabilitiesmvp.mapper;

import org.aitu.vulnerabilitiesmvp.dto.auth.RegisterResponse;
import org.aitu.vulnerabilitiesmvp.entity.Account;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public RegisterResponse toRegisterResponse(User user, Account account) {
        return new RegisterResponse(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            account.getId(),
            account.getCurrency(),
            user.getCreatedAt()
        );
    }
}
