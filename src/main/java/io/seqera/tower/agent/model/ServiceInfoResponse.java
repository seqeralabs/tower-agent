/*
 * Copyright 2021-2026, Seqera.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.seqera.tower.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.micronaut.core.annotation.ReflectiveAccess;

import java.util.Objects;

/**
 * ServiceInfoResponse
 */
@ReflectiveAccess
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