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
package org.eurekastreams.server.action.execution.feed;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.eurekastreams.commons.actions.ExecutionStrategy;
import org.eurekastreams.commons.actions.context.ActionContext;
import org.eurekastreams.commons.exceptions.ExecutionException;
import org.eurekastreams.commons.logging.LogFactory;
import org.eurekastreams.server.action.request.feed.RefreshFeedRequest;
import org.eurekastreams.server.domain.DomainGroup;
import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.GeneralGadgetDefinition;
import org.eurekastreams.server.domain.Person;
import org.eurekastreams.server.domain.gadgetspec.GadgetMetaDataDTO;
import org.eurekastreams.server.domain.stream.Activity;
import org.eurekastreams.server.domain.stream.ActivityVerb;
import org.eurekastreams.server.domain.stream.BaseObjectType;
import org.eurekastreams.server.domain.stream.plugins.Feed;
import org.eurekastreams.server.domain.stream.plugins.FeedSubscriber;
import org.eurekastreams.server.persistence.mappers.FindByIdMapper;
import org.eurekastreams.server.persistence.mappers.InsertMapper;
import org.eurekastreams.server.persistence.mappers.UpdateMapper;
import org.eurekastreams.server.persistence.mappers.cache.CacheKeys;
import org.eurekastreams.server.persistence.mappers.cache.MemcachedCache;
import org.eurekastreams.server.persistence.mappers.requests.FindByIdRequest;
import org.eurekastreams.server.persistence.mappers.requests.PersistenceRequest;
import org.eurekastreams.server.service.actions.strategies.activity.plugins.ObjectMapper;
import org.eurekastreams.server.service.actions.strategies.activity.plugins.SpecificUrlObjectMapper;
import org.eurekastreams.server.service.actions.strategies.activity.plugins.rome.ActivityStreamsModule;
import org.eurekastreams.server.service.actions.strategies.activity.plugins.rome.ActivityStreamsModuleImpl;
import org.eurekastreams.server.service.actions.strategies.activity.plugins.rome.FeedFactory;
import org.eurekastreams.server.service.opensocial.gadgets.spec.GadgetMetaDataFetcher;

import com.sun.syndication.feed.module.SyModule;
import com.sun.syndication.feed.module.SyModuleImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Goes out to the interwebs, grabs a feed, parses it, stores it in DB and queues it up to be stored in cache. The
 * reason for the queueing is so I don't update the same exact list 100s of times instead of once. This cuts down by
 * multiple orders of magnitude
 * 
 */
public class RefreshFeedExecution implements ExecutionStrategy<ActionContext>
{
    /**
     * The number of milliseconds in a minute.
     */
    private static final int MS_IN_MIN = 60000;
    /**
     * The number of minutes in an hour.
     */
    private static final int MINS_IN_HOUR = 60;
    /**
     * The number of hours in a day.
     */
    private static final int HOURS_IN_DAY = 24;
    /**
     * The number of days in a week.
     */
    private static final int DAYS_IN_WEEK = 7;
    /**
     * The number of days in a month.
     */
    private static final int DAYS_IN_MONTH = 31;
    /**
     * Tne number of days in a year.
     */
    private static final int DAYS_IN_YEAR = 365;

    /**
     * Logger.
     */
    private Log log = LogFactory.make();

    /**
     * Standard feed mappers.
     */
    private HashMap<BaseObjectType, ObjectMapper> standardFeedMappers;

    /**
     * Mappers for specific websites.
     */
    private List<SpecificUrlObjectMapper> specificUrlMappers;

    /**
     * Bulk insert activity into the DB.
     */
    private InsertMapper<Activity> activityDBInserter;

    /**
     * The cache.
     */
    private MemcachedCache cache;

    /**
     * Feed fetcher factory, really only needed for testing.
     */
    private FeedFactory feedFetcherFactory;

    /**
     * Person finder.
     */
    private FindByIdMapper<Person> personFinder;
    /**
     * Group finder.
     */
    private FindByIdMapper<DomainGroup> groupFinder;

    /**
     * Feed finder.
     */
    private FindByIdMapper<Feed> feedFinder;

    /**
     * Meta data fetcher.
     */
    private GadgetMetaDataFetcher metaDataFetcher = null;

    /**
     * Update mapper.
     */
    private UpdateMapper<Feed> updateFeedMapper = null;

    /**
     * Default constructor.
     * 
     * @param inStandardFeedMappers
     *            Standard feed mappers.
     * @param inSpecificUrlMappers
     *            Mappers for specific websites.
     * @param inActivityDBInserter
     *            Insert activity into the DB.
     * @param inCache
     *            The cache.
     * @param inFeedFetcherFactory
     *            feed fetcher factory.
     * @param inPersonFinder
     *            find person.
     * @param inGroupFinder
     *            find group.
     * @param inFeedFinder
     *            find feed.
     * @param inMetaDataFetcher
     *            fetcher.
     * @param inUpdateFeedMapper
     *            updateMapper.
     */
    public RefreshFeedExecution(final HashMap<BaseObjectType, ObjectMapper> inStandardFeedMappers,
            final List<SpecificUrlObjectMapper> inSpecificUrlMappers,
            final InsertMapper<Activity> inActivityDBInserter, final MemcachedCache inCache,
            final FeedFactory inFeedFetcherFactory, final FindByIdMapper<Person> inPersonFinder,
            final FindByIdMapper<DomainGroup> inGroupFinder, final FindByIdMapper<Feed> inFeedFinder,
            final GadgetMetaDataFetcher inMetaDataFetcher, final UpdateMapper<Feed> inUpdateFeedMapper)
    {
        standardFeedMappers = inStandardFeedMappers;
        specificUrlMappers = inSpecificUrlMappers;
        activityDBInserter = inActivityDBInserter;
        cache = inCache;
        feedFetcherFactory = inFeedFetcherFactory;
        personFinder = inPersonFinder;
        groupFinder = inGroupFinder;
        feedFinder = inFeedFinder;
        metaDataFetcher = inMetaDataFetcher;
        updateFeedMapper = inUpdateFeedMapper;
    }

    /**
     * {@inheritDoc}.
     * 
     * Grab all the feeds, set them as pending, and fire off an async job. to refresh each one.
     */
    public Serializable execute(final ActionContext inActionContext) throws ExecutionException
    {
        boolean foundSpecificMapper = false;
        Long updateFrequency = null;
        List<Activity> insertedActivities = new LinkedList<Activity>();
        ArrayList<Long> insertedActivityIds = new ArrayList<Long>();

        RefreshFeedRequest request = (RefreshFeedRequest) inActionContext.getParams();
        Feed feed = feedFinder.execute(new FindByIdRequest("Feed", request.getFeedId()));
        Date lastPostDate = feed.getLastPostDate();
        ArrayList<Long> people = new ArrayList<Long>();
        ArrayList<Long> groups = new ArrayList<Long>();
        try
        {
            URL feedUrl = new URL(feed.getUrl());
            SyndFeed syndFeed = feedFetcherFactory.getSyndicatedFeed(feedUrl);
            SyModuleImpl syMod = (SyModuleImpl) syndFeed.getModule(SyModule.URI);

            if (syMod != null)
            {
                updateFrequency = getUpdateFrequency(syMod.getUpdatePeriod(), syMod.getUpdateFrequency());
            }

            ObjectMapper selectedObjectMapper = null;
            for (SpecificUrlObjectMapper strategy : specificUrlMappers)
            {
                Matcher match = Pattern.compile(strategy.getRegex()).matcher(feed.getUrl());
                if (match.find())
                {
                    selectedObjectMapper = strategy.getObjectMapper();
                    foundSpecificMapper = true;
                    break;
                }
            }

            for (Object entryObject : syndFeed.getEntries())
            {
                try
                {
                    SyndEntryImpl entry = (SyndEntryImpl) entryObject;
                    if (feed.getLastPostDate() == null || entry.getPublishedDate().after(feed.getLastPostDate()))
                    {
                        if (lastPostDate == null || entry.getPublishedDate().after(lastPostDate))
                        {
                            lastPostDate = entry.getPublishedDate();
                        }

                        Activity activity = new Activity();
                        activity.setAppType(EntityType.PLUGIN);
                        activity.setAppId(feed.getPlugin().getId());
                        activity.setAppSource(feed.getUrl());
                        final Map<String, GeneralGadgetDefinition> gadgetDefs =
                            new HashMap<String, GeneralGadgetDefinition>();
                        gadgetDefs.put(feed.getPlugin().getUrl(), feed.getPlugin());
                        try
                        {
                            List<GadgetMetaDataDTO> meta = metaDataFetcher.getGadgetsMetaData(gadgetDefs);

                            if (meta.size() > 0)
                            {
                                activity.setAppName(meta.get(0).getTitle());
                            }
                        }
                        catch (Exception ex)
                        {
                            log.error("Error getting plugin definition");
                            activity.setAppName(feed.getTitle());
                        }
                        activity.setPostedTime(entry.getPublishedDate());
                        activity.setUpdated(entry.getUpdatedDate());
                        activity.setVerb(ActivityVerb.POST);
                        if (!foundSpecificMapper)
                        {
                            ActivityStreamsModule activityModule = (ActivityStreamsModuleImpl) entry
                                    .getModule(ActivityStreamsModule.URI);
                            BaseObjectType type = feed.getPlugin().getObjectType();

                            if (activityModule != null)
                            {
                                type = BaseObjectType.valueOf(activityModule.getObjectType());
                                entry = activityModule.getAtomEntry();
                            }

                            if (!standardFeedMappers.containsKey(type))
                            {
                                type = BaseObjectType.NOTE;
                            }
                            selectedObjectMapper = standardFeedMappers.get(type);
                        }
                        activity.setBaseObjectType(selectedObjectMapper.getBaseObjectType());
                        activity.setBaseObject(selectedObjectMapper.getBaseObject(entry));
                        for (FeedSubscriber feedSubscriber : feed.getFeedSubscribers())
                        {
                            Activity activityForIndividual = (Activity) activity.clone();

                            if (feedSubscriber.getEntityType().equals(EntityType.PERSON))
                            {
                                Person person = personFinder.execute(new FindByIdRequest("Person", feedSubscriber
                                        .getEntityId()));
                                activityForIndividual.setActorType(EntityType.PERSON);
                                activityForIndividual.setActorId(person.getAccountId());
                                activityForIndividual.setRecipientParentOrg(person.getParentOrganization());
                                activityForIndividual.setRecipientStreamScope(person.getStreamScope());
                                activityForIndividual.setIsDestinationStreamPublic(true);
                                people.add(person.getId());
                            }
                            else if (feedSubscriber.getEntityType().equals(EntityType.GROUP))
                            {
                                DomainGroup group = groupFinder.execute(new FindByIdRequest("DomainGroup",
                                        feedSubscriber.getEntityId()));

                                activityForIndividual.setActorType(EntityType.GROUP);
                                activityForIndividual.setActorId(group.getShortName());
                                activityForIndividual.setRecipientParentOrg(group.getParentOrganization());
                                activityForIndividual.setRecipientStreamScope(group.getStreamScope());
                                activityForIndividual.setIsDestinationStreamPublic(group.isPublicGroup());
                                groups.add(group.getId());
                            }
                            insertedActivities.add(activityForIndividual);
                        }
                    } // end if
                }
                catch (Exception ex)
                {
                    log.warn("ATOM/RSS entry is not to spec. "
                            + "Skipping entry and moving to the next one. Feed url: " + feed.getUrl(), ex);
                }
            }

            // updateFeedMapper.execute(new PersistenceRequest<Feed>(feed));
            Collections.reverse(insertedActivities);
            if (insertedActivities.size() > 0)
            {
                for (Activity activity : insertedActivities)
                {
                    activityDBInserter.execute(new PersistenceRequest<Activity>(activity));
                    insertedActivityIds.add(activity.getId());
                }
                Collections.reverse(insertedActivityIds);

                // TODO: this is not performant; fix
                if (cache.get(CacheKeys.BUFFERED_ACTIVITIES) == null)
                {
                    cache.setList(CacheKeys.BUFFERED_ACTIVITIES, insertedActivityIds);
                }
                else
                {
                    cache.addToTopOfList(CacheKeys.BUFFERED_ACTIVITIES, insertedActivityIds);
                }
            }
        }
        catch (Exception ex)
        {
            log.error("Error retrieving feed: " + feed.getUrl());
        }
        finally
        {
            feed.setLastPostDate(lastPostDate);
            feed.setLastUpdated(new Date().getTime() / MS_IN_MIN);
            feed.setUpdateFrequency(updateFrequency);
            feed.setPending(false);
        }

        return null;
    }

    /**
     * Get the update frequeuncy in minutes given the period and frequency.
     * 
     * @param updatePeriod
     *            Period, hourly, daily, weekly, etc.
     * @param updateFrequency
     *            Frequency, if daily, 1 would be 1 day, if hourly, 1 would be 1 hour.
     * @return the frequency in minutes.
     */
    private long getUpdateFrequency(final String updatePeriod, final int updateFrequency)
    {
        // this can't be a switch statement due to Java lameness :(
        if (updatePeriod.equals(SyModule.HOURLY))
        {
            return updateFrequency * MINS_IN_HOUR;
        }
        else if (updatePeriod.equals(SyModule.DAILY))
        {
            return updateFrequency * MINS_IN_HOUR * HOURS_IN_DAY;
        }
        else if (updatePeriod.equals(SyModule.WEEKLY))
        {
            return updateFrequency * MINS_IN_HOUR * HOURS_IN_DAY * DAYS_IN_WEEK;
        }
        else if (updatePeriod.equals(SyModule.MONTHLY))
        {
            return updateFrequency * MINS_IN_HOUR * HOURS_IN_DAY * DAYS_IN_MONTH;
        }
        else if (updatePeriod.equals(SyModule.YEARLY))
        {
            return updateFrequency * MINS_IN_HOUR * HOURS_IN_DAY * DAYS_IN_YEAR;
        }

        // default to hourly.
        return updateFrequency * MINS_IN_HOUR;
    }
}
