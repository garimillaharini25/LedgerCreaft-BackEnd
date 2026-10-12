package com.expensetracker;

import com.expensetracker.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseIntegrationTest {

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected String getDemoUserToken() {
        return "Bearer " + jwtTokenProvider.generateTokenFromEmail("demo@expensetracker.com", 1L, "Demo User");
    }

    protected String getUser2Token() {
        return "Bearer " + jwtTokenProvider.generateTokenFromEmail("user2@expensetracker.com", 2L, "Sarah Jenkins");
    }
}
