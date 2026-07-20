package com.ashraf.service;

import com.ashraf.entity.User;

public record RefreshTokenResult(String rawToken, User user) {}