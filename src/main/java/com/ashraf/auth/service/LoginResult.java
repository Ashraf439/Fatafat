package com.ashraf.auth.service;

import com.ashraf.core.entity.User;

public record LoginResult(String accessToken, String rawRefreshToken, User user) {}