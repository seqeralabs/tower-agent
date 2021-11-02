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

import java.util.Objects;

/**
 * ServiceInfoResponse
 */
@JsonPropertyOrder({
        ServiceInfoResponse.JSON_PROPERTY_SERVICE_INFO
})

public class ServiceInfoResponse {

    public static final String JSON_PROPERTY_SERVICE_INFO = "serviceInfo";
    private ServiceInfo serviceInfo;


    public ServiceInfoResponse serviceInfo(ServiceInfo serviceInfo) {

        this.serviceInfo = serviceInfo;
        return this;
    }

    /**
     * Get serviceInfo
     *
     * @return serviceInfo
     **/
    @JsonProperty(JSON_PROPERTY_SERVICE_INFO)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }


    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInfoResponse serviceInfoResponse = (ServiceInfoResponse) o;
        return Objects.equals(this.serviceInfo, serviceInfoResponse.serviceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceInfo);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ServiceInfoResponse {\n");
        sb.append("    serviceInfo: ").append(toIndentedString(serviceInfo)).append("\n");
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