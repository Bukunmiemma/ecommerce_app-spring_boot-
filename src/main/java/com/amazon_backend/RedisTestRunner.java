package com.amazon_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTestRunner implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTestRunner(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        redisTemplate.opsForValue().set("test-key", "hello redis");
        System.out.println(redisTemplate.opsForValue().get("test-key"));
    }
}
