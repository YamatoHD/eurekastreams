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
package org.eurekastreams.web.client.ui.common.stream.comments;

import org.eurekastreams.commons.client.ActionRequestImpl;
import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.search.modelview.CommentDTO;
import org.eurekastreams.web.client.events.CommentAddedEvent;
import org.eurekastreams.web.client.events.Observer;
import org.eurekastreams.web.client.events.data.GotSystemSettingsResponseEvent;
import org.eurekastreams.web.client.model.SystemSettingsModel;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.TimerFactory;
import org.eurekastreams.web.client.ui.TimerHandler;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Background;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Size;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * Post comment panel.
 * 
 */
public class PostCommentPanel extends FlowPanel
{
    /**
     * Max comment length.
     */
    private static final int MAXLENGTH = 250;

    /**
     * Delay before processing onBlur event. This prevents weird things like blocking click events on other elements due
     * to the shifting page.
     */
    private static final int BLUR_DELAY = 500;

    /**
     * Whether or not it's inactive.
     */
    private boolean inactive = true;
    /**
     * The count down label.
     */
    private Label countDown = new Label();
    /**
     * The comment box.
     */
    private TextArea commentBox;
    /**
     * The post button.
     */
    private Hyperlink post;
    /**
     * The message id.
     */
    private Long messageId;

    /**
     * If the fake text box shouldn't show when unactivated.
     */
    private boolean fullCollapse;

    /**
     * Default constructor.
     * 
     * @param inMessageId
     *            the message id.
     * @param inFullCollapse
     *            if the panel should fully collapse on unActivate.
     */
    public PostCommentPanel(final Long inMessageId, final boolean inFullCollapse)
    {
        messageId = inMessageId;
        this.addStyleName("message-comment");
        unActivate();
        fullCollapse = inFullCollapse;
    }

    /**
     * Unactivate the control.
     */
    private void unActivate()
    {
        if (!fullCollapse)
        {
            this.clear();
            Label postAComment = new Label("post a comment");
            postAComment.addStyleName("unactive simulated-text-box");
            postAComment.addClickHandler(new ClickHandler()
            {
                public void onClick(final ClickEvent event)
                {
                    activate();
                }
            });
            add(postAComment);
        }
        else
        {
            this.setVisible(false);
        }
    }

    /**
     * Activate the control.
     */
    public void activate()
    {
        this.clear();
        this.setVisible(true);
        Widget avatar = new AvatarWidget(Session.getInstance().getCurrentPerson().getId(), Session.getInstance()
                .getCurrentPerson().getAvatarId(), EntityType.PERSON, Size.VerySmall, Background.White);
        avatar.addStyleName("avatar");
        this.add(avatar);

        FlowPanel body = new FlowPanel();
        body.addStyleName("body");

        commentBox = new TextArea();
        body.add(commentBox);
        commentBox.setFocus(true);

        countDown = new Label(Integer.toString(MAXLENGTH));
        countDown.addStyleName("characters-remaining");
        body.add(countDown);

        post = new Hyperlink("post", History.getToken());
        post.addStyleName("post-button");
        post.addStyleName("inactive");

        final Label warning = new Label();
        warning.addStyleName("warning hidden");
        body.add(warning);

        Session.getInstance().getEventBus().addObserver(GotSystemSettingsResponseEvent.class,
                new Observer<GotSystemSettingsResponseEvent>()
                {
                    public void update(final GotSystemSettingsResponseEvent event)
                    {
                        String text = event.getResponse().getContentWarningText();
                        if (text != null && !text.isEmpty())
                        {
                            warning.removeStyleName("hidden");
                            warning.setText(text);
                        }
                    }

                });

        SystemSettingsModel.getInstance().fetch(null, true);

        post.addClickHandler(new ClickHandler()
        {
            public void onClick(final ClickEvent event)
            {
                fullCollapse = false;
                if (!inactive)
                {
                    unActivate();
                    CommentDTO comment = new CommentDTO();
                    comment.setBody(commentBox.getText());
                    comment.setActivityId(messageId);
                    Session.getInstance().getActionProcessor().makeRequest(
                            new ActionRequestImpl<CommentDTO>("postActivityCommentAction", comment),
                            new AsyncCallback<CommentDTO>()
                            {
                                /* implement the async call back methods */
                                public void onFailure(final Throwable caught)
                                {

                                }

                                public void onSuccess(final CommentDTO result)
                                {
                                    Session.getInstance().getEventBus().notifyObservers(
                                            new CommentAddedEvent(result, messageId));
                                }
                            });
                }
            }
        });
        body.add(post);

        this.add(body);
        commentBox.setFocus(true);
        nativeSetFocus(commentBox.getElement());

        commentBox.addBlurHandler(new BlurHandler()
        {
            public void onBlur(final BlurEvent arg0)
            {
                TimerFactory timerFactory = new TimerFactory();
                timerFactory.runTimer(BLUR_DELAY, new TimerHandler()
                {
                    public void run()
                    {
                        if (commentBox.getText().length() == 0)
                        {
                            unActivate();
                        }
                    }
                });
            }
        });

        commentBox.addKeyPressHandler(new KeyPressHandler()
        {
            public void onKeyPress(final KeyPressEvent event)
            {
                if (event.getCharCode() == KeyCodes.KEY_ESCAPE)
                {
                    unActivate();
                }
            }
        });

        commentBox.addKeyUpHandler(new KeyUpHandler()
        {
            public void onKeyUp(final KeyUpEvent event)
            {
                onCommentChanges();
            }
        });

        commentBox.addChangeHandler(new ChangeHandler()
        {
            public void onChange(final ChangeEvent event)
            {
                onCommentChanges();
            }
        });
    }

    /**
     * Set the focus.
     * 
     * @param el
     *            the element
     */
    public static native void nativeSetFocus(final Element el) /*-{
                                                           el.focus();
                                                    }-*/;

    /**
     * Gets triggered whenever the comment changes.
     */
    private void onCommentChanges()
    {
        Integer charsRemaining = MAXLENGTH - commentBox.getText().length();
        countDown.setText(charsRemaining.toString());
        if (charsRemaining >= 0 && charsRemaining != MAXLENGTH)
        {
            countDown.removeStyleName("over-character-limit");
            post.removeStyleName("inactive");
            inactive = false;
        }
        else
        {
            if (charsRemaining != MAXLENGTH)
            {
                countDown.addStyleName("over-character-limit");

            }
            post.addStyleName("inactive");
            inactive = true;
        }
    }
}
