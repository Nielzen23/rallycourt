package com.rallycourt.auth.service;

import com.rallycourt.auth.entity.User;

public interface CourtOwnerService {

    User apply();

    User approve(Long userId);

    User reject(Long userId);
}
