/*
 * Copyright (c) 2010 Lockheed Martin Corporation
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
package org.eurekastreams.server.action.execution.stream;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.eurekastreams.commons.actions.ExecutionStrategy;
import org.eurekastreams.commons.actions.context.PrincipalActionContext;
import org.eurekastreams.commons.logging.LogFactory;
import org.eurekastreams.server.action.request.stream.GetFlaggedActivitiesByOrgRequest;
import org.eurekastreams.server.domain.PagedSet;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.persistence.mappers.stream.GetFlaggedActivitiesForOrganization;

/**
 * Returns the flagged activities for a given organization.
 */
public class GetFlaggedActivitiesForOrganizationExecution implements ExecutionStrategy<PrincipalActionContext>
{
    /**
     * Logger.
     */
    private Log log = LogFactory.make();

    /** Mapper. */
    private GetFlaggedActivitiesForOrganization mapper;

    /**
     * Constructor.
     *
     * @param inMapper
     *            Mapper.
     */
    public GetFlaggedActivitiesForOrganizationExecution(final GetFlaggedActivitiesForOrganization inMapper)
    {
        mapper = inMapper;
    }

    /**
     * Get a paged set of flagged activities directly under an organization.
     *
     * @param inActionContext
     *            the action context having with GetFlaggedActivitiesByOrgRequest param
     * @return a paged set of flagged activities directly under an organization.
     */
    @Override
    public Serializable execute(final PrincipalActionContext inActionContext)
    {
        String userName = inActionContext.getPrincipal().getAccountId();
        GetFlaggedActivitiesByOrgRequest request = (GetFlaggedActivitiesByOrgRequest) inActionContext.getParams();

        if (log.isInfoEnabled())
        {
            log.info("scoping the request for flagged activities with the requesting user account: " + userName);
        }
        request.setRequestingUserAccountId(userName);

        if (log.isInfoEnabled())
        {
            log.info("Getting flagged activities directly under org with id: " + request.getOrganizationId());
        }

        PagedSet<ActivityDTO> activities = mapper.execute(request);
        if (log.isInfoEnabled())
        {
            log.info("Found " + activities.getPagedSet().size() + " activities");
        }
        return activities;
    }
}
