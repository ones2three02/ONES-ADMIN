package com.ones.admin.auth;

import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoginFailureAtomicityTest {

    @Autowired
    private SystemUserMapper userMapper;

    @Test
    void concurrentFailuresAreIncrementedAtomically() throws Exception {
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername("atomic-login-" + System.nanoTime());
        user.setDisplayName("登录并发测试用户");
        user.setPasswordHash("not-used");
        user.setEnabled(true);
        user.setFailedLoginCount(0);
        userMapper.insert(user);

        int attempts = 12;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);
        try {
            for (int attempt = 0; attempt < attempts; attempt++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        userMapper.incrementLoginFailure(user.getId(), 5, lockedUntil);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
            SystemUserEntity updated = userMapper.selectById(user.getId());
            assertThat(updated.getFailedLoginCount()).isEqualTo(attempts);
            assertThat(updated.getLockedUntil()).isNotNull();
        } finally {
            executor.shutdownNow();
            userMapper.deleteById(user.getId());
        }
    }
}
