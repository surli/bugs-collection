/*
Copyright (c) 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.Ip;
import org.ovirt.engine.api.model.IpVersion;

/**
 * A collection of static methods useful for dealing with IP addresses.
 */
public class IpHelper {
    /**
     * Determines the IP version used by an {@link Ip} object, either taking it directly if it is explicitly specified
     * or inferring it from the {@code address} property.
     *
     * @param ip the object containing the IP configuration
     * @return the IP version, or {@code null} if it can't be determined
     */
    public static IpVersion getVersion(Ip ip) {
        // Return null if no IP configuration was given:
        if (ip == null) {
            return null;
        }

        // If the IP version is explicitly specified, then use it:
        if (ip.isSetVersion()) {
            return ip.getVersion();
        }

        // If there is no explicit IP version then we can try to infer it from the address, but only if it has been
        // provided:
        String address = ip.getAddress();
        if (address == null) {
            return null;
        }

        // This isn't a complete validation, as the API doesn't currently perform validations, but assuming that the
        // address is valid, checking if it contains a colon is enough to determine if it is IPv4 or IPv6, as no valid
        // IPv4 address can contain a colon, and all valid IPv6 addresses must contain at least one colon.
        return address.contains(":")? IpVersion.V6: IpVersion.V4;
    }
}
