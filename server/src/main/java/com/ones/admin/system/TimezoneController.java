package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/timezone")
@Tag(name = "系统管理-时区")
public class TimezoneController {

    private static final String TIMEZONE_KEY = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    @GetMapping("/getTimezoneOptions")
    @Operation(summary = "获取可选时区")
    public ApiResult<List<TimezoneOption>> getTimezoneOptions() {
        List<TimezoneOption> options = ZoneId.getAvailableZoneIds().stream()
                .sorted(Comparator.naturalOrder())
                .map(zoneId -> new TimezoneOption(zoneId, zoneId))
                .toList();
        return ApiResult.ok(options);
    }

    @GetMapping("/getTimezone")
    @Operation(summary = "获取当前时区")
    public ApiResult<String> getTimezone() {
        Object timezone = StpUtil.getSession().get(TIMEZONE_KEY);
        return ApiResult.ok(timezone == null ? DEFAULT_TIMEZONE : String.valueOf(timezone));
    }

    @PostMapping("/setTimezone")
    @Operation(summary = "设置当前时区")
    public ApiResult<Void> setTimezone(@Valid @RequestBody TimezoneRequest request) {
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "timezone 参数不正确");
        }
        StpUtil.getSession().set(TIMEZONE_KEY, request.timezone());
        return ApiResult.ok(null);
    }

    public record TimezoneOption(String label, String value) {
    }

    public record TimezoneRequest(
            @NotBlank
            @Size(max = 64)
            String timezone
    ) {
    }
}
