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
package org.eurekastreams.server.persistence.mappers.db;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.eurekastreams.server.persistence.mappers.MapperTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ibm.icu.util.Calendar;

/**
 * Tests mapper to get unread application alerts by date.
 */
public class GetUserIdsWithUnreadApplicationAlertsByDateTest extends MapperTest
{
    /**
     * System under test.
     */
    @Autowired
    private GetUserIdsWithUnreadApplicationAlertsByDate sut;

    /**
     * Test execute method.
     */
    @Test
    public void testExecute()
    {
        final Long userId = 99L;
        final int year = 2010;
        final int day = 31;
        Date date = new GregorianCalendar(year, Calendar.JANUARY, day).getTime();

        List<Long> results = sut.execute(date);
        assertEquals(1, results.size());
        assertEquals(userId, results.get(0));
    }
}
