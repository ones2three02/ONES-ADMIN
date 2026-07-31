package com.ones.admin.auth.oauth;

import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Component
public class ThirdPartyAuthProviderRegistry {

    private final Map<String, ThirdPartyAuthProvider> providers;

    public ThirdPartyAuthProviderRegistry(List<ThirdPartyAuthProvider> providers) {
        Map<String, ThirdPartyAuthProvider> indexedProviders = new LinkedHashMap<>();
        providers.forEach(provider -> {
            if (indexedProviders.putIfAbsent(provider.id(), provider) != null) {
                throw new IllegalStateException("第三方认证 Provider 重复: " + provider.id());
            }
        });
        this.providers = Collections.unmodifiableMap(indexedProviders);
    }

    public List<ThirdPartyAuthProvider> all() {
        return List.copyOf(providers.values());
    }

    public ThirdPartyAuthProvider requireEnabled(String providerId) {
        ThirdPartyAuthProvider provider = providers.get(providerId);
        if (provider == null || !provider.enabled()) {
            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_NOT_ENABLED);
        }
        return provider;
    }
}
