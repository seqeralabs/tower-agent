/*
 * Copyright (c) 2021, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */

package io.seqera.tower.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.micronaut.core.annotation.ReflectiveAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ServiceInfo
 */
@ReflectiveAccess
@JsonPropertyOrder({
        ServiceInfo.JSON_PROPERTY_VERSION,
        ServiceInfo.JSON_PROPERTY_API_VERSION,
        ServiceInfo.JSON_PROPERTY_COMMIT_ID,
        ServiceInfo.JSON_PROPERTY_AUTH_TYPES,
        ServiceInfo.JSON_PROPERTY_LOGIN_PATH,
        ServiceInfo.JSON_PROPERTY_HEARTBEAT_INTERVAL,
        ServiceInfo.JSON_PROPERTY_ALLOW_INSTANCE_CREDENTIALS,
        ServiceInfo.JSON_PROPERTY_LANDING_URL
})
public class ServiceInfo {
    public static final String JSON_PROPERTY_VERSION = "version";
    private String version;

    public static final String JSON_PROPERTY_API_VERSION = "apiVersion";
    private String apiVersion;

    public static final String JSON_PROPERTY_COMMIT_ID = "commitId";
    private String commitId;

    public static final String JSON_PROPERTY_AUTH_TYPES = "authTypes";
    private List<String> authTypes = null;

    public static final String JSON_PROPERTY_LOGIN_PATH = "loginPath";
    private String loginPath;

    public static final String JSON_PROPERTY_HEARTBEAT_INTERVAL = "heartbeatInterval";
    private Integer heartbeatInterval;

    public static final String JSON_PROPERTY_ALLOW_INSTANCE_CREDENTIALS = "allowInstanceCredentials";
    private Boolean allowInstanceCredentials;

    public static final String JSON_PROPERTY_LANDING_URL = "landingUrl";
    private String landingUrl;


    public ServiceInfo version(String version) {

        this.version = version;
        return this;
    }

    /**
     * Get version
     *
     * @return version
     **/
    @JsonProperty(JSON_PROPERTY_VERSION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getVersion() {
        return version;
    }


    public void setVersion(String version) {
        this.version = version;
    }


    public ServiceInfo apiVersion(String apiVersion) {

        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Get apiVersion
     *
     * @return apiVersion
     **/
    @JsonProperty(JSON_PROPERTY_API_VERSION)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getApiVersion() {
        return apiVersion;
    }


    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }


    public ServiceInfo commitId(String commitId) {

        this.commitId = commitId;
        return this;
    }

    /**
     * Get commitId
     *
     * @return commitId
     **/
    @JsonProperty(JSON_PROPERTY_COMMIT_ID)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getCommitId() {
        return commitId;
    }


    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }


    public ServiceInfo authTypes(List<String> authTypes) {

        this.authTypes = authTypes;
        return this;
    }

    public ServiceInfo addAuthTypesItem(String authTypesItem) {
        if (this.authTypes == null) {
            this.authTypes = new ArrayList<>();
        }
        this.authTypes.add(authTypesItem);
        return this;
    }

    /**
     * Get authTypes
     *
     * @return authTypes
     **/
    @JsonProperty(JSON_PROPERTY_AUTH_TYPES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public List<String> getAuthTypes() {
        return authTypes;
    }


    public void setAuthTypes(List<String> authTypes) {
        this.authTypes = authTypes;
    }


    public ServiceInfo loginPath(String loginPath) {

        this.loginPath = loginPath;
        return this;
    }

    /**
     * Get loginPath
     *
     * @return loginPath
     **/
    @JsonProperty(JSON_PROPERTY_LOGIN_PATH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getLoginPath() {
        return loginPath;
    }


    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public ServiceInfo heartbeatInterval(Integer heartbeatInterval) {

        this.heartbeatInterval = heartbeatInterval;
        return this;
    }

    /**
     * Get heartbeatInterval
     *
     * @return heartbeatInterval
     **/
    @JsonProperty(JSON_PROPERTY_HEARTBEAT_INTERVAL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }


    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }


    public ServiceInfo allowInstanceCredentials(Boolean allowInstanceCredentials) {

        this.allowInstanceCredentials = allowInstanceCredentials;
        return this;
    }

    /**
     * Get allowInstanceCredentials
     *
     * @return allowInstanceCredentials
     **/
    @JsonProperty(JSON_PROPERTY_ALLOW_INSTANCE_CREDENTIALS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public Boolean getAllowInstanceCredentials() {
        return allowInstanceCredentials;
    }


    public void setAllowInstanceCredentials(Boolean allowInstanceCredentials) {
        this.allowInstanceCredentials = allowInstanceCredentials;
    }


    public ServiceInfo landingUrl(String landingUrl) {

        this.landingUrl = landingUrl;
        return this;
    }

    /**
     * Get landingUrl
     *
     * @return landingUrl
     **/
    @JsonProperty(JSON_PROPERTY_LANDING_URL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public String getLandingUrl() {
        return landingUrl;
    }


    public void setLandingUrl(String landingUrl) {
        this.landingUrl = landingUrl;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInfo serviceInfo = (ServiceInfo) o;
        return Objects.equals(this.version, serviceInfo.version) &&
                Objects.equals(this.apiVersion, serviceInfo.apiVersion) &&
                Objects.equals(this.commitId, serviceInfo.commitId) &&
                Objects.equals(this.authTypes, serviceInfo.authTypes) &&
                Objects.equals(this.loginPath, serviceInfo.loginPath) &&
                Objects.equals(this.heartbeatInterval, serviceInfo.heartbeatInterval) &&
                Objects.equals(this.allowInstanceCredentials, serviceInfo.allowInstanceCredentials) &&
                Objects.equals(this.landingUrl, serviceInfo.landingUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, apiVersion, commitId, authTypes, loginPath, heartbeatInterval, allowInstanceCredentials, landingUrl);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ServiceInfo {\n");
        sb.append("    version: ").append(toIndentedString(version)).append("\n");
        sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
        sb.append("    commitId: ").append(toIndentedString(commitId)).append("\n");
        sb.append("    authTypes: ").append(toIndentedString(authTypes)).append("\n");
        sb.append("    loginPath: ").append(toIndentedString(loginPath)).append("\n");
        sb.append("    heartbeatInterval: ").append(toIndentedString(heartbeatInterval)).append("\n");
        sb.append("    allowInstanceCredentials: ").append(toIndentedString(allowInstanceCredentials)).append("\n");
        sb.append("    landingUrl: ").append(toIndentedString(landingUrl)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}