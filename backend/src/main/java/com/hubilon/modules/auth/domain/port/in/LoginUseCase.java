package com.hubilon.modules.auth.domain.port.in;

import com.hubilon.modules.auth.application.dto.LoginCommand;
import com.hubilon.modules.auth.domain.model.TokenPair;

public interface LoginUseCase {

    TokenPair login(LoginCommand command);
}
