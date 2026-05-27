package com.rallycourt.activity.exception;

public class ActivityUserNotFoundException extends RuntimeException {

    public ActivityUserNotFoundException(Long userId) {
        super("User not found for activity lookup: " + userId);
    }
}
