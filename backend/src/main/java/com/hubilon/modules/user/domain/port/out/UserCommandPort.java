package com.hubilon.modules.user.domain.port.out;

import com.hubilon.modules.user.domain.model.User;

public interface UserCommandPort {

    User save(User user);
}
