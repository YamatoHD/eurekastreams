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
package org.eurekastreams.server.action.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eurekastreams.server.persistence.mappers.GetOrgCoordinators;
import org.eurekastreams.server.persistence.mappers.GetRootOrganizationIdAndShortName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for IsRootOrganizationCoordinatorStrategy class.
 * 
 */
public class IsRootOrganizationCoordinatorTest
{
    /**
     * Context for building mock objects.
     */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * Root org id DAO.
     */
    private GetRootOrganizationIdAndShortName rootOrgIdDAO = context.mock(GetRootOrganizationIdAndShortName.class);

    /**
     * Org coordinator ids DAO.
     */
    private GetOrgCoordinators orgCoordinatorIdsDAO = context.mock(GetOrgCoordinators.class);

    /**
     * System under test.
     */
    private IsRootOrganizationCoordinator sut;

    /**
     * User id.
     */
    private Long userId = 1L;

    /**
     * RootOrgId id.
     */
    private Long rootOrgId = 2L;

    /**
     * Pre-test setup.
     */
    @Before
    public void setUp()
    {
        sut = new IsRootOrganizationCoordinator(rootOrgIdDAO, orgCoordinatorIdsDAO);
    }

    /**
     * Test.
     */
    @Test
    public void testTrue()
    {
        final Set<Long> coordIds = new HashSet<Long>(2);
        coordIds.add(userId);
        coordIds.add(9L);
        context.checking(new Expectations()
        {
            {
                allowing(rootOrgIdDAO).getRootOrganizationId();
                will(returnValue(rootOrgId));

                allowing(orgCoordinatorIdsDAO).execute(rootOrgId);
                will(returnValue(coordIds));
            }
        });

        assertTrue(sut.isRootOrganizationCoordinator(userId));
        context.assertIsSatisfied();
    }

    /**
     * Test.
     */
    @Test
    public void testFalse()
    {
        final Set<Long> coordIds = new HashSet<Long>(2);
        coordIds.add(8L);
        coordIds.add(9L);
        context.checking(new Expectations()
        {
            {
                allowing(rootOrgIdDAO).getRootOrganizationId();
                will(returnValue(rootOrgId));

                allowing(orgCoordinatorIdsDAO).execute(rootOrgId);
                will(returnValue(coordIds));
            }
        });

        assertFalse(sut.isRootOrganizationCoordinator(userId));
        context.assertIsSatisfied();
    }
}
