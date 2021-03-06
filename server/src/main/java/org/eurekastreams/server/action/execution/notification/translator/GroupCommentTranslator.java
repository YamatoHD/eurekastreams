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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eurekastreams.server.domain.NotificationDTO;
import org.eurekastreams.server.domain.NotificationType;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamEntityDTO;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.db.GetCommentorIdsByActivityId;
import org.eurekastreams.server.persistence.mappers.stream.GetCoordinatorIdsByGroupId;

/**
 * Translates the event of someone commenting on a post to appropriate notifications.
 */
public class GroupCommentTranslator implements NotificationTranslator
{
    /**
     * Mapper to get commentors.
     */
    GetCommentorIdsByActivityId commentorsMapper;

    /**
     * Mapper to get activity details.
     */
    DomainMapper<List<Long>, List<ActivityDTO>> activitiesMapper;

    /**
     * Mapper to get group coordinator ids.
     */
    private GetCoordinatorIdsByGroupId coordinatorMapper;

    /**
     * Constructor.
     * 
     * @param inCommentorsMapper
     *            commentors mapper to set.
     * @param inActivitiesMapper
     *            activities mapper to set.
     * @param inCoordinatorMapper
     *            coordinator mapper to set.
     */
    public GroupCommentTranslator(final GetCommentorIdsByActivityId inCommentorsMapper,
            final DomainMapper<List<Long>, List<ActivityDTO>> inActivitiesMapper,
            final GetCoordinatorIdsByGroupId inCoordinatorMapper)
    {
        commentorsMapper = inCommentorsMapper;
        activitiesMapper = inActivitiesMapper;
        coordinatorMapper = inCoordinatorMapper;
    }

    /**
     * Gets a list of people to notify when a new comment is added.
     * 
     * @param inActorId
     *            ID of actor that made the comment.
     * @param inDestinationId
     *            ID of group whose stream contains the activity commented on.
     * @param inActivityId
     *            the activity id itself.
     * @return List of notifications generated.
     */
    @Override
    public Collection<NotificationDTO> translate(final long inActorId, final long inDestinationId,
            final long inActivityId)
    {
        List<NotificationDTO> notifications = new ArrayList<NotificationDTO>();

        ActivityDTO activity = activitiesMapper.execute(Arrays.asList(inActivityId)).get(0);
        if (activity != null)
        {
            Map<NotificationType, List<Long>> recipients = new HashMap<NotificationType, List<Long>>();

            // Adds post author as recipient
            long postAuthor = activity.getActor().getId();
            if (postAuthor != inActorId)
            {
                recipients.put(NotificationType.COMMENT_TO_PERSONAL_POST, Collections.singletonList(postAuthor));
            }

            // Adds group coordinators as recipients (if enabled)
            List<Long> coordinatorIds = Collections.EMPTY_LIST;
            if (coordinatorMapper != null)
            {
                coordinatorIds = coordinatorMapper.execute(inDestinationId);
                List<Long> coordinatorsToNotify = new ArrayList<Long>();
                for (long coordinatorId : coordinatorIds)
                {
                    if (coordinatorId != postAuthor && coordinatorId != inActorId)
                    {
                        coordinatorsToNotify.add(coordinatorId);
                        // this recipient list will keep replacing the old value in the map when new recipients are
                        // found
                        recipients.put(NotificationType.COMMENT_TO_GROUP_STREAM, coordinatorsToNotify);
                    }
                }
            }

            // Adds recipient who previously commented on this post
            List<Long> commentToCommentedRecipients = new ArrayList<Long>();
            for (long commentorId : commentorsMapper.execute(inActivityId))
            {
                if (commentorId != postAuthor && !coordinatorIds.contains(commentorId) && commentorId != inActorId)
                {
                    commentToCommentedRecipients.add(commentorId);

                    // this recipient list will keep replacing the old value in the map when new recipients are found
                    recipients.put(NotificationType.COMMENT_TO_COMMENTED_POST, commentToCommentedRecipients);
                }
            }

            for (NotificationType notificationType : recipients.keySet())
            {
                NotificationDTO notif = new NotificationDTO(recipients.get(notificationType), notificationType,
                        inActorId);
                notif.setActivity(inActivityId, activity.getBaseObjectType());
                StreamEntityDTO dest = activity.getDestinationStream();
                notif.setDestination(dest.getId(), dest.getType(), dest.getUniqueIdentifier(), dest.getDisplayName());
                if (notif.getType().equals(NotificationType.COMMENT_TO_COMMENTED_POST))
                {
                    StreamEntityDTO author = activity.getActor();
                    notif.setAuxiliary(author.getType(), author.getUniqueIdentifier(), author.getDisplayName());
                }
                notifications.add(notif);
            }
        }
        return notifications;
    }
}
