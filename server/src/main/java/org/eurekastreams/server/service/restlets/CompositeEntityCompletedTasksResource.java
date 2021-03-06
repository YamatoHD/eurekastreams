/*
 * Copyright (c) 2009-2010 Lockheed Martin Corporation
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
package org.eurekastreams.server.service.restlets;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eurekastreams.server.domain.CompositeEntity;
import org.eurekastreams.server.domain.Task;
import org.eurekastreams.server.persistence.CompositeEntityMapper;
import org.eurekastreams.server.persistence.TaskMapper;
import org.restlet.data.Request;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;

/**
 * The collection resource for tasks. Only supports POST.
 * 
 */
public class CompositeEntityCompletedTasksResource extends WritableResource
{
    /**
     * Logger.
     */
    private Log log = LogFactory.getLog(CompositeEntityCompletedTasksResource.class);

    /**
     * the entity shortname.
     */
    private String entityShortName;

    /**
     * the gadget def id.
     */
    private Long gadgetDefId;
    /**
     * the entity mapper.
     */
    private CompositeEntityMapper entityMapper;
    /**
     * the task mapper.
     */
    private TaskMapper taskMapper;

    /**
     * Initialize parameters from the request object. the context of the request
     * 
     * @param request
     *            the client's request
     */
    @Override
    protected void initParams(final Request request)
    {
        entityShortName = (String) request.getAttributes().get("shortName");
        gadgetDefId = Long.decode((String) request.getAttributes().get("gadgetDefId"));
    }

    /**
     * The Entity Mapper. Used by tests.
     * 
     * @param inMapper
     *            mapper.
     */
    public void setEntityMapper(final CompositeEntityMapper inMapper)
    {
        entityMapper = inMapper;
    }

    /**
     * The Task Mapper. Used by tests.
     * 
     * @param inTaskMapper
     *            mapper.
     */
    public void setTaskMapper(final TaskMapper inTaskMapper)
    {
        taskMapper = inTaskMapper;
    }

    /**
     * POST a task ot the completed list (complete a task).
     * 
     * @param entity
     *            the JSON.
     * @throws ResourceException
     *             error.
     */
    @Override
    public void acceptRepresentation(final Representation entity) throws ResourceException
    {
        CompositeEntity myOrg = entityMapper.findByShortName(entityShortName);
        JSONObject jsonObject;
        try
        {
            jsonObject = JSONObject.fromObject(entity.getText());
            String taskName = jsonObject.getString("name");

            Task completedTask = taskMapper.findByNameAndGadgetDefId(taskName, gadgetDefId);
            myOrg.getCompletedTasks().add(completedTask);
            entityMapper.flush();

        }
        catch (IOException e)
        {
            log.error(e);
        }
    }
}
