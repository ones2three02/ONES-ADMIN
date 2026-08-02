package com.ones.admin.common;

import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.code.ErrorCode;
import com.ones.admin.hr.HrErrorCode;
import com.ones.admin.system.SystemErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeGovernanceTest {

    @Test
    void errorCodesAreUniqueAcrossModules() {
        List<Integer> codes = allErrorCodes()
                .map(ErrorCode::code)
                .toList();

        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    void moduleErrorCodesUseReservedRanges() {
        assertThat(Arrays.stream(AuthErrorCode.values()).map(AuthErrorCode::code))
                .allMatch(code -> code >= 4200 && code < 4300);
        assertThat(Arrays.stream(SystemErrorCode.values()).map(SystemErrorCode::code))
                .allMatch(code -> code >= 4100 && code < 4400);
        assertThat(Arrays.stream(HrErrorCode.values()).map(HrErrorCode::code))
                .allMatch(code -> code >= 4400 && code < 4500);
    }

    private Stream<ErrorCode> allErrorCodes() {
        return Stream.of(
                        Arrays.stream(CommonErrorCode.values()).map(ErrorCode.class::cast),
                        Arrays.stream(AuthErrorCode.values()).map(ErrorCode.class::cast),
                        Arrays.stream(SystemErrorCode.values()).map(ErrorCode.class::cast),
                        Arrays.stream(HrErrorCode.values()).map(ErrorCode.class::cast)
                )
                .flatMap(stream -> stream);
    }
}
