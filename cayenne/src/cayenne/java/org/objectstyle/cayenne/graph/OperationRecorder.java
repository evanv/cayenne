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
package org.objectstyle.cayenne.graph;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;

/**
 * Stores individual graph changes as GraphDiff "operations" that can be replayed later.
 * Optionally can broadcast GraphEvents via supplied event manager.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class OperationRecorder implements GraphChangeHandler {

    protected List diffs;
    protected int commitStartMarker;

    // event stuff
    protected EventSubject eventSubject;
    protected Object eventSource;
    protected EventManager eventManager;

    protected boolean eventsEnabled;

    /**
     * Creates an OperationRecorder that stores graph changes as GraphDiffs and DOES NOT
     * broadcast GraphEvents.
     */
    public OperationRecorder() {
        this.eventsEnabled = false;
        clear();
    }

    /**
     * Creates an OperationRecorder that stores graph changes as GraphDiffs and broadcasts
     * GraphEvents.
     * 
     * @param eventManager required
     * @param eventSubject required
     * @param eventSource optional. OperationRecorder will use self as an event source if
     *            this argument is null.
     */
    public OperationRecorder(EventManager eventManager, EventSubject eventSubject,
            Object eventSource) {

        this();
        initForEvents(eventManager, eventSubject, eventSource);
    }

    public void initForEvents(
            EventManager eventManager,
            EventSubject eventSubject,
            Object eventSource) {
        // sanity check - make sure we can send events
        if (eventManager == null) {
            throw new IllegalArgumentException("Null eventManager");
        }

        if (eventSubject == null) {
            throw new IllegalArgumentException("Null eventSubject");
        }

        if (eventSource == null) {
            eventSource = this;
        }

        this.eventManager = eventManager;
        this.eventSource = eventSource;
        this.eventSubject = eventSubject;
        this.eventsEnabled = true;
    }

    public boolean isEventsSupported() {
        return eventManager != null && eventSource != null && eventSubject != null;
    }

    /**
     * Returns true if events broadcasting is explicitly enabled and EventManager is not
     * null.
     */
    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    /**
     * Enables events. Throws an exception if an attempt is made to enable events when
     * they are not supported.
     */
    public void setEventsEnabled(boolean eventsEnabled) {

        if (eventsEnabled && !isEventsSupported()) {
            throw new CayenneRuntimeException(
                    "Can't enable events - OperationRecorder is not configured to send them.");
        }

        this.eventsEnabled = eventsEnabled;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Object getEventSource() {
        return eventSource;
    }

    public EventSubject getEventSubject() {
        return eventSubject;
    }

    /**
     * Returns a combined GraphDiff for all recorded operations.
     */
    public GraphDiff getDiffs() {
        return new CompoundDiff(immutableList(0, diffs.size()));
    }

    /**
     * "Forgets" all stored operations.
     */
    public void clear() {
        // must create a new list instead of clearing an existing one, as the original
        // list may have been exposed via events or "getDiffs", and trimming it is
        // undesirable.
        this.diffs = new ArrayList();
        this.commitStartMarker = -1;
    }

    public int size() {
        return diffs.size();
    }

    public boolean isEmpty() {
        return diffs.isEmpty();
    }

    public boolean isCommitInProgress() {
        return commitStartMarker >= 0;
    }

    // ***** GraphChangeHandler methods ******
    // =======================================

    /**
     * Clears commit marker, but keeps all recorded operations.
     */
    public void graphCommitAborted() {
        commitStartMarker = -1;

        // TODO (Andrus 10/11/2005) send an event?
    }

    /**
     * Calls "clear()", removing all memorized changes. Posts a GraphEvent with graph diff
     * containing changes since the last commit or rollback.
     */
    public void graphCommitStarted() {
        if (isCommitInProgress()) {
            throw new CayenneRuntimeException(
                    "Attempt to start commit while commit is in progress.");
        }

        commitStartMarker = diffs.size();

        if (isEventsEnabled()) {
            // include all diffs up to this point
            GraphEvent e = new GraphEvent(eventSource, new GraphStateChange(
                    GraphStateChange.COMMIT_STARTED,
                    immutableList(0, diffs.size())));
            eventManager.postEvent(e, eventSubject);
        }
    }

    /**
     * Calls "clear()", removing all memorized changes. Posts an event with graph diff
     * containing changes introduced after the last call to "graphCommitStarted".
     */
    public void graphCommitted() {
        if (isEventsEnabled()) {
            List eventDiffs = commitStartMarker == diffs.size() ? null : immutableList(
                    commitStartMarker,
                    diffs.size());

            // note that "clear" creates a new list, so it is safe to use the original
            // list to store event data
            clear();

            // include all diffs after the commit start marker.
            GraphEvent e = new GraphEvent(eventSource, new GraphStateChange(
                    GraphStateChange.COMMITTED,
                    eventDiffs));
            eventManager.postEvent(e, eventSubject);
        }
        else {
            clear();
        }
    }

    /**
     * Calls "clear()", removing all memorized changes.
     */
    public void graphRolledback() {

        if (isEventsEnabled()) {
            List eventDiffs = immutableList(0, diffs.size());

            // note that "clear" creates a new list, so it is safe to use the original
            // list to store event data
            clear();

            GraphEvent e = new GraphEvent(eventSource, new GraphStateChange(
                    GraphStateChange.ROLLEDBACK,
                    eventDiffs));
            eventManager.postEvent(e, eventSubject);
        }
        else {
            clear();
        }
    }

    public void nodeCreated(Object nodeId) {
        processOperation(new NodeCreateOperation(nodeId));
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        processOperation(new NodeIdChangeOperation(nodeId, newId));
    }

    public void nodeRemoved(Object nodeId) {
        processOperation(new NodeDeleteOperation(nodeId));
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        processOperation(new NodePropertyChangeOperation(
                nodeId,
                property,
                oldValue,
                newValue));
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        processOperation(new ArcCreateOperation(nodeId, targetNodeId, arcId));
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        processOperation(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
    }

    void processOperation(GraphDiff operation) {
        diffs.add(operation);

        if (isEventsEnabled()) {
            GraphEvent e = new GraphEvent(eventSource, operation);
            eventManager.postEvent(e, eventSubject);
        }
    }

    /**
     * Returns a sublist of the diffs list that shouldn't change when OperationRecorder is
     * cleared or new operations are added.
     */
    List immutableList(int fromIndex, int toIndex) {
        if (toIndex - fromIndex == 0) {
            return Collections.EMPTY_LIST;
        }

        // Assuming that internal diffs list can only grow and can never be trimmed,
        // return sublist will never change - something that callers are expecting
        return Collections.unmodifiableList(new SubList(diffs, fromIndex, toIndex));
    }

    // moded Sublist from JDK that doesn't check for co-modification, as the underlying
    // list is guaranteed to only grow and never shrink or be replaced.
    class SubList extends AbstractList {

        private List list;
        private int offset;
        private int size;

        SubList(List list, int fromIndex, int toIndex) {
            if (fromIndex < 0)
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > list.size()) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex("
                        + fromIndex
                        + ") > toIndex("
                        + toIndex
                        + ")");
            }
            this.list = list;
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        public Object get(int index) {
            rangeCheck(index);
            return list.get(index + offset);
        }

        public int size() {
            return size;
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ",Size: " + size);
            }
        }
    }
}
