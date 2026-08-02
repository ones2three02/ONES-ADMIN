package com.ones.admin.auth.oauth;

import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;

public interface ThirdPartyAuthProvider {

    String id();

    String displayName();

    boolean enabled();

    String authorizationUrl(String state);

    ExternalIdentityProfile authenticate(String authorizationCode);
}
