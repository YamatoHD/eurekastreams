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
package org.eurekastreams.web.client.ui.common.avatar;

import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.Page;
import org.eurekastreams.web.client.history.CreateUrlRequest;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Background;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Size;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.Panel;

import java.util.HashMap;

/**
 * Displays an avatar and makes it link to the proper item's profile page.
 */
public class AvatarLinkPanel extends Composite
{
    /**
     * Constructor.
     * 
     * @param entityType
     *            Type of entity the avatar belongs to.
     * @param entityUniqueId
     *            Short name / account id of entity the avatar belongs to.
     * @param avatar
     *            Avatar image widget.
     */
    public AvatarLinkPanel(final EntityType entityType, final String entityUniqueId, final AvatarWidget avatar)
    {
        Panel main = new FlowPanel();
        main.addStyleName("avatar");
        initWidget(main);

        Page page;
        switch (entityType)
        {
        case PERSON:
            page = Page.PEOPLE;
            break;
        case GROUP:
            page = Page.GROUPS;
            break;
        case ORGANIZATION:
            page = Page.ORGANIZATIONS;
            break;
        default:
            // this should never happen
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("tab", "Stream");

        String linkUrl = Session.getInstance().generateUrl(new CreateUrlRequest(page, entityUniqueId, params));

        Hyperlink link = new InlineHyperlink("", linkUrl);
        main.add(link);
        link.getElement().appendChild(avatar.getElement());
    }

    /**
     * Constructor.
     * 
     * @param entityId
     *            the entity ID.
     * @param entityUniqueId
     *            Short name / account id of entity the avatar belongs to.
     * @param avatarId
     *            the ID of the avatar.
     * @param entityType
     *            the entity type.
     * @param size
     *            the avatar size.
     */
    public AvatarLinkPanel(final EntityType entityType, final String entityUniqueId, final long entityId,
            final String avatarId, final Size size)
    {
        this(entityType, entityUniqueId, new AvatarWidget(entityId, avatarId, entityType, size));
    }

    /**
     * Constructor.
     * 
     * @param entityId
     *            the entity ID.
     * @param entityUniqueId
     *            Short name / account id of entity the avatar belongs to.
     * @param avatarId
     *            the ID of the avatar.
     * @param entityType
     *            the entity type.
     * @param size
     *            the avatar size.
     * @param bg
     *            the background color.
     */
    @Deprecated
    public AvatarLinkPanel(final EntityType entityType, final String entityUniqueId, final long entityId,
            final String avatarId, final Size size, final Background bg)
    {
        this(entityType, entityUniqueId, new AvatarWidget(entityId, avatarId, entityType, size));
    }
}
