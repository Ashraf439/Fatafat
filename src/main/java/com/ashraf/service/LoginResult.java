package com.ashraf.service;

import com.ashraf.entity.User;

public record LoginResult(String accessToken, String rawRefreshToken, User user) {}