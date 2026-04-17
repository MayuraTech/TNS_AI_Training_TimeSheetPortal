package com.tns.tms.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGeneratorTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "Admin@123!";
        String hash = encoder.encode(password);
        System.out.println("=== HASH FOR DB ===");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Verify existing: " + encoder.matches(password, "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
        System.out.println("===================");
    }
}
