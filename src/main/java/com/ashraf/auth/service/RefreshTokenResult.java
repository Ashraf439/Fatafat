package com.ashraf.auth.service;

import com.ashraf.core.entity.User;

public record RefreshTokenResult(String rawToken, User user) {}