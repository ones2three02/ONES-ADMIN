package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.web.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/timezone")
public class TimezoneController {

    private static final String TIMEZONE_KEY = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    @GetMapping("/getTimezoneOptions")
    public ApiResult<List<TimezoneOption>> getTimezoneOptions() {
        List<TimezoneOption> options = ZoneId.getAvailableZoneIds().stream()
                .sorted(Comparator.naturalOrder())
                .map(zoneId -> new TimezoneOption(zoneId, zoneId))
                .toList();
        return ApiResult.ok(options);
    }

    @GetMapping("/getTimezone")
    public ApiResult<String> getTimezone() {
        Object timezone = StpUtil.getSession().get(TIMEZONE_KEY);
        return ApiResult.ok(timezone == null ? DEFAULT_TIMEZONE : String.valueOf(timezone));
    }

    @PostMapping("/setTimezone")
    public ApiResult<Void> setTimezone(@RequestBody TimezoneRequest request) {
        ZoneId.of(request.timezone());
        StpUtil.getSession().set(TIMEZONE_KEY, request.timezone());
        return ApiResult.ok(null);
    }

    public record TimezoneOption(String label, String value) {
    }

    public record TimezoneRequest(String timezone) {
    }
}
