/*
* Copyright (c) 2014 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.restapi.resource.validation;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.utils.InvalidValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This mapper is intended to handle the exceptions thrown by the JAXB message body reader.
 */
@Provider
public class IOExceptionMapper implements ExceptionMapper<IOException> {
    private static final Logger log = LoggerFactory.getLogger(IOExceptionMapper.class);

    @Context
    protected UriInfo uriInfo;
    @Context
    protected Request request;

    @Override
    public Response toResponse(IOException exception) {
        // Check if the cause of the exception is an invalid value, and generate a specific error message:
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidValueException) {
            Fault fault = new Fault();
            fault.setReason("Invalid value");
            fault.setDetail(cause.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(fault).build();
        }

        // For any other kind of exception generate an error response with information describing the correct syntax for
        // the request:
        try {
            log.error(
                "IO exception while processing \"{}\" request for path \"{}\"",
                request.getMethod(),
                uriInfo.getPath()
            );
            log.error("Exception", exception);
            UsageFinder finder = new UsageFinder();
            return Response.status(Status.BAD_REQUEST).entity(finder.getUsageMessage(uriInfo, request)).build();
        }
        catch (Exception error) {
            throw new WebApplicationException(error, Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
    }

}
