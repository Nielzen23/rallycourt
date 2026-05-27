package com.rallycourt.court.service;

import com.rallycourt.auth.entity.User;
import com.rallycourt.court.entity.Court;

public interface CourtAccessService {

    User getCurrentUser();

    void verifyCanCreateCourt(User user);

    void verifyCanUpdateCourt(User user, Court court);
}
