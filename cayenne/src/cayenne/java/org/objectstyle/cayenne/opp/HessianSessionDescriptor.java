/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.opp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.event.EventBridgeFactory;
import org.objectstyle.cayenne.event.EventManager;

/**
 * A descriptor passed from HessianService to the caller when a session is established. It
 * provides the client with details on how to invoke the service and how to listen for the
 * server events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class HessianSessionDescriptor implements Serializable {

    static final Collection SUBJECTS = Arrays.asList(new Object[] {
            OPPChannel.GRAPH_CHANGED_SUBJECT, OPPChannel.GRAPH_COMMITTED_SUBJECT,
            OPPChannel.GRAPH_ROLLEDBACK_SUBJECT
    });

    protected String name;
    protected String sessionId;

    protected String eventBridgeFactory;
    protected Map eventBridgeParameters;

    transient EventBridge eventBridge;

    // private constructor used by hessian deserialization mechanism
    private HessianSessionDescriptor() {

    }

    /**
     * Creates a HessianServiceDescriptor without server events support.
     */
    public HessianSessionDescriptor(String sessionId) {
        this(sessionId, null, null);
    }

    /**
     * Creates a HessianServiceDescriptor with server events support.
     */
    public HessianSessionDescriptor(String sessionId, String eventBridgeFactory,
            Map eventBridgeParameters) {

        if (sessionId == null) {
            throw new IllegalArgumentException("Null sessionId");
        }

        this.sessionId = sessionId;
        this.eventBridgeFactory = eventBridgeFactory;
        this.eventBridgeParameters = eventBridgeParameters;
    }

    public int hashCode() {
        return new HashCodeBuilder(71, 5).append(sessionId).toHashCode();
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns session group name. Group name is used for shared sessions.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isServerEventsEnabled() {
        return eventBridgeFactory != null;
    }

    /**
     * Returns true if EventBridge is started.
     */
    public boolean isListeningForServerEvents() {
        return eventBridge != null;
    }

    /**
     * Starts an EventBridge that will listen for server events. Does nothing if server
     * events support is not configured in the descriptor.
     * 
     * @throws CayenneRuntimeException if EventBridge startup fails for any reason.
     */
    public void startListeningForServerEvents(EventManager eventManager)
            throws CayenneRuntimeException {

        // shutdown old bridge...
        stopListeningForServerEvents();

        if (!isServerEventsEnabled()) {
            return;
        }

        try {
            EventBridgeFactory factory = (EventBridgeFactory) Class.forName(
                    eventBridgeFactory).newInstance();

            Map parameters = eventBridgeParameters != null
                    ? eventBridgeParameters
                    : Collections.EMPTY_MAP;

            this.eventBridge = factory.createEventBridge(SUBJECTS, sessionId, parameters);
            eventBridge.startup(eventManager, EventBridge.RECEIVE_EXTERNAL);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating EventBridge.", ex);
        }

    }

    public void stopListeningForServerEvents() throws CayenneRuntimeException {

        if (this.eventBridge != null) {

            try {
                this.eventBridge.shutdown();
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error shutting down EventBridge", e);
            }
            finally {
                this.eventBridge = null;
            }
        }
    }

    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this)
                .append("sessionId", sessionId);

        if (eventBridgeFactory != null) {
            builder.append("eventBridgeFactory", eventBridgeFactory);
        }

        if (name != null) {
            builder.append("name", name);
        }

        return builder.toString();
    }

}
