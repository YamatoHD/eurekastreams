/*
 * Copyright (c) 2009 Lockheed Martin Corporation
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
import java.util.Date;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.eurekastreams.server.domain.Person;
import org.eurekastreams.server.persistence.PersonMapper;

/**
 * A restlet resource representing a Person's biography.
 */
public class PersonFullBioResource extends WritableResource
{
    /**
     * Logger.
     */
    private Log log = LogFactory.getLog(PersonFullBioResource.class);

    /**
     * The key used in JSON.
     */
    public static final String BIOGRAPHY_KEY = "bio";

    /**
     * The key used in JSON.
     */
    public static final String OVERVIEW_KEY = "overview";
    
    /**
     * Mapper used to look up the person.
     */
    private PersonMapper personMapper;

    /**
     * The uuid that this resource represents.
     */
    private String uuid = null;

    /**
     * Initialize parameters from the request object.
     *            the context of the request
     * @param request
     *            the client's request
     */
    protected void initParams(final Request request)
    {
        uuid = (String) request.getAttributes().get("uuid");
    }

    /**
     * Handle GET requests.
     * 
     * @param variant
     *            the variant to provide.
     * @return the representation of the requested resource
     */
    public Representation represent(final Variant variant)
    {
        Person person = personMapper.findByOpenSocialId(uuid);

        JSONObject json = new JSONObject();
        json.put(BIOGRAPHY_KEY, person.getBiography());
        json.put(OVERVIEW_KEY, person.getOverview());

        log.debug("Returning the JSON for Biography: " + json.toString());
        Representation rep = new StringRepresentation(json.toString(), MediaType.APPLICATION_JSON);
        rep.setExpirationDate(new Date(0L));
        return rep;
    }

    /**
     * Handle PUT requests.
     * 
     * @param entity
     *            the resource's new representation
     * @throws ResourceException
     *             hopefully not
     */
    @Override
    public void storeRepresentation(final Representation entity) throws ResourceException
    {
        Person person = personMapper.findByOpenSocialId(uuid);

        try
        {
            JSONObject jsonObject = JSONObject.fromObject(entity.getText());
            String bio = jsonObject.getString(BIOGRAPHY_KEY);
            String overview = jsonObject.getString(OVERVIEW_KEY);
            person.setBiography(bio);
            person.setOverview(overview);
            personMapper.flush();
        }
        catch (IOException e)
        {
            log.error("Unable to read update to overview for " + uuid, e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Error with the provided Representation");
        }
        catch (Exception ex)
        {
            log.error("Unable to store updated biography/overview", ex);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Error storing the provided Representation");
        }
    }

    /**
     * Setter. 
     * @param inPersonMapper the personMapper to set
     */
    public void setPersonMapper(final PersonMapper inPersonMapper)
    {
        this.personMapper = inPersonMapper;
    }
}
