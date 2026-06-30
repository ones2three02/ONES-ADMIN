package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceRuleResponse(
        String permissionCodePattern,
        String operationIdPattern,
        List<Rule> rules
) {

    public record Rule(
            String ruleCode,
            String severity,
            boolean blocking,
            String category,
            String description,
            String remediation
    ) {
    }
}
