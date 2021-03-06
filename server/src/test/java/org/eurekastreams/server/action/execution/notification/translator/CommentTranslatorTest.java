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
package org.eurekastreams.server.action.execution.notification.translator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eurekastreams.server.domain.NotificationDTO;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamEntityDTO;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.db.GetCommentorIdsByActivityId;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the comment notification translator.
 */
public class CommentTranslatorTest
{
    /** Context for building mock objects. */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /** Mock commentors mapper. */
    private GetCommentorIdsByActivityId commentorsMapper = context.mock(GetCommentorIdsByActivityId.class);

    /** Mock activities mapper. */
    private DomainMapper<List<Long>, List<ActivityDTO>>  activitiesMapper = context.mock(DomainMapper.class);

    /** System under test. */
    private CommentTranslator sut;

    /** Test data. */
    private static final long ACTOR_ID = 1111L;

    /** Test data. */
    private static final long STREAM_OWNER_ID = 2222L;

    /** Test data. */
    private static final long DESTINATION_ID = 3333L;

    /** Test data. */
    private static final long ACTIVITY_ID = 4444L;

    /** Test data. */
    private static final long COMMENTOR = 5555L;

    /**
     * Setup test.
     */
    @Before
    public void setup()
    {
        sut = new CommentTranslator(commentorsMapper, activitiesMapper);
    }

    /**
     * Test the translator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTranslate()
    {
        final StreamEntityDTO actor = new StreamEntityDTO();
        actor.setId(STREAM_OWNER_ID);

        final ActivityDTO activity = new ActivityDTO();
        activity.setActor(actor);

        context.checking(new Expectations()
        {
            {
                oneOf(activitiesMapper).execute(Arrays.asList(ACTIVITY_ID));
                will(returnValue(Arrays.asList(activity)));

                oneOf(commentorsMapper).execute(ACTIVITY_ID);
                will(returnValue(Collections.singletonList(COMMENTOR)));
            }
        });

        Collection<NotificationDTO> results = sut.translate(ACTOR_ID, DESTINATION_ID, ACTIVITY_ID);
        assertEquals(3, results.size());
        context.assertIsSatisfied();
    }
}
