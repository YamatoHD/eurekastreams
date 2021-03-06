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
package org.eurekastreams.server.persistence.mappers.cache;

import java.util.ArrayList;
import java.util.List;

import org.eurekastreams.server.persistence.mappers.chained.RefreshStrategy;
import org.eurekastreams.server.persistence.mappers.stream.CachedDomainMapper;
import org.eurekastreams.server.persistence.mappers.stream.GetOrganizationsByIds;

/**
 * Refreshes cached org activity.
 */
public class OrgActivityIdsRefresher extends CachedDomainMapper implements RefreshStrategy<Long, List<Long>>
{
    /**
     * DAO for looking up an Organization.
     */
    private GetOrganizationsByIds organizationDAO;

    /**
     * @param inOrganizationDAO
     *            the organizationDAO to set.
     */
    public void setOrganizationDAO(final GetOrganizationsByIds inOrganizationDAO)
    {
        this.organizationDAO = inOrganizationDAO;
    }

    /**
     * Returns compositeStreamId for a person's parent org.
     * 
     * @param inOrgId
     *            the parent org id.
     * @return compositeStreamId for a person's parent org.
     */
    private Long getPersonParentOrgCompositeStreamId(final long inOrgId)
    {
        ArrayList<Long> ids = new ArrayList<Long>()
        {
            {
                add(inOrgId);
            }
        };
        return organizationDAO.execute(ids).get(0).getCompositeStreamId();
    }

    /**
     * @param inOrgId
     *            the org to refresh.
     * @param inActivities
     *            the updated activity.
     */
    public void refresh(final Long inOrgId, final List<Long> inActivities)
    {
        getCache().setList(CacheKeys.ACTIVITIES_BY_COMPOSITE_STREAM + getPersonParentOrgCompositeStreamId(inOrgId),
                inActivities);
    }

}
