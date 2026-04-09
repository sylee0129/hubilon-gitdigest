package com.hubilon.modules.user.domain.port.in;

import com.hubilon.modules.user.application.dto.UserRegisterCommand;
import com.hubilon.modules.user.application.dto.UserRegisterResult;

public interface UserRegisterUseCase {

    UserRegisterResult register(UserRegisterCommand command);
}
