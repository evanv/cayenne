/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

package org.objectstyle.cayenne.unittest;

/**
 * Helper class allowing unit tests to wait till a code in question
 * executes in a separate thread. There is still some element of uncertainty remains,
 * since this implementation simply tries to give other threads enough time to execute,
 * instead of watching for threads activity. 
 * 
 * <p>Note that result sampling is done every 300 ms., so if the test succeeds earlier,
 * test case wouldn't have to wait for the whole time period specified by timeout.</p>
 * 
 * @author Andrei Adamchik
 */
public abstract class ThreadedTestHelper {

    protected abstract void assertResult() throws Exception;

    public void assertWithTimeout(long timeoutMs) throws Exception {
        long checkEveryXMs;
        int maxMumberOfChecks;

        if (timeoutMs < 300) {
            maxMumberOfChecks = 1;
            checkEveryXMs = timeoutMs;
        }
        else {
            maxMumberOfChecks = Math.round(timeoutMs / 300.00f);
            checkEveryXMs = 300;
        }

        // TODO: for things asserting that a certain event DID NOT happen
        // we need a better implementation, that should probably sleep for 
        // the whole timeout interval, since otherwise we may have a false
        // positive (i.e. assertion succeeded not because a certain thing did not
        // happen, but rather cause it happened after the assertion was run).

        // for now lets wait for at least one time slice to decrease 
        // the possibility of false positives
        Thread.sleep(checkEveryXMs);
        maxMumberOfChecks--;

        // wait 5 seconds at the most (10 times 0.5 sec.)
        for (int i = 0; i < maxMumberOfChecks; i++) {
            try {
                assertResult();

                // success... return immediately
                return;
            }
            catch (Throwable th) {
                // wait some more
                Thread.sleep(checkEveryXMs);
            }
        }

        // if it throws, it throws...
        assertResult();
    }
}
