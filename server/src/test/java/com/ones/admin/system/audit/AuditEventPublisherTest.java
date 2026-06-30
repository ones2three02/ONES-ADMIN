package com.ones.admin.system.audit;

import com.ones.admin.common.event.SystemEventPayload;
import com.ones.admin.common.event.SystemEventPublisher;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import com.ones.admin.system.mapper.SystemOperationLogMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventPublisherTest {

    @Test
    void operationAuditPublishesSystemEventAfterPersistence() {
        SystemOperationLogMapper mapper = mock(SystemOperationLogMapper.class);
        SystemEventPublisher publisher = mock(SystemEventPublisher.class);
        OperationAuditService service = new OperationAuditService(mapper, publisher);

        service.record(new OperationAuditService.OperationAuditRecord(
                1L,
                "POST",
                "/api/system/users",
                "系统管理-用户",
                "新增用户",
                "system:user:create",
                true,
                0,
                null,
                "trace-operation",
                "127.0.0.1",
                "JUnit",
                12L
        ));

        verify(publisher).publish(argThat(event ->
                "audit.operation.recorded".equals(event.eventType())
                        && "ones.audit.operation.recorded".equals(event.routingKey())
                        && "trace-operation".equals(event.traceId())
                        && "/api/system/users".equals(event.payload().get("path"))
                        && Boolean.TRUE.equals(event.payload().get("success"))
        ));
    }

    @Test
    void loginAuditPublishesSystemEventAfterPersistence() {
        SystemLoginLogMapper mapper = mock(SystemLoginLogMapper.class);
        SystemEventPublisher publisher = mock(SystemEventPublisher.class);
        LoginAuditService service = new LoginAuditService(mapper, publisher);
        AuditRequestContext context = new AuditRequestContext("127.0.0.1", "JUnit", "trace-login");

        service.recordFailure("ghost", "用户名或密码错误", context);

        verify(publisher).publish(argThat((SystemEventPayload event) ->
                "audit.login.recorded".equals(event.eventType())
                        && "ones.audit.login.recorded".equals(event.routingKey())
                        && "trace-login".equals(event.traceId())
                        && "ghost".equals(event.payload().get("username"))
                        && Boolean.FALSE.equals(event.payload().get("success"))
                        && assertFailureReason(event)
        ));
    }

    private boolean assertFailureReason(SystemEventPayload event) {
        assertThat(event.payload().get("failureReason")).isEqualTo("用户名或密码错误");
        return true;
    }
}
