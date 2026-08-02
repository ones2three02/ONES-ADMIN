package com.ones.admin.common.repeatsubmit;

import java.time.Duration;

public interface RepeatSubmitTicketStore {

    boolean tryLock(String key, Duration ttl);
}
