package com.rallycourt.auth.service;

import com.rallycourt.auth.entity.User;

public interface SessionTokenService {

    String issueSessionToken(User user);

    User validateSessionToken(String sessionToken);
}
