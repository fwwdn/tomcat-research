/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote.http11;

import org.apache.coyote.Processor;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.tomcat.util.net.AprEndpoint.Poller;
import org.apache.tomcat.util.net.SocketWrapperBase;


/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class Http11AprProtocol extends AbstractHttp11Protocol<Long> {

    private static final Log log = LogFactory.getLog(Http11AprProtocol.class);

    public Http11AprProtocol() {
        super(new AprEndpoint());
        Http11ConnectionHandler cHandler = new Http11ConnectionHandler(this);
        setHandler(cHandler);
        ((AprEndpoint) getEndpoint()).setHandler(cHandler);
    }


    @Override
    protected Log getLog() { return log; }

    @Override
    public boolean isAprRequired() {
        // Override since this protocol implementation requires the APR/native
        // library
        return true;
    }

    public int getPollTime() { return ((AprEndpoint)getEndpoint()).getPollTime(); }
    public void setPollTime(int pollTime) { ((AprEndpoint)getEndpoint()).setPollTime(pollTime); }

    public void setPollerSize(int pollerSize) { getEndpoint().setMaxConnections(pollerSize); }
    public int getPollerSize() { return getEndpoint().getMaxConnections(); }

    public int getSendfileSize() { return ((AprEndpoint)getEndpoint()).getSendfileSize(); }
    public void setSendfileSize(int sendfileSize) { ((AprEndpoint)getEndpoint()).setSendfileSize(sendfileSize); }

    public void setSendfileThreadCount(int sendfileThreadCount) { ((AprEndpoint)getEndpoint()).setSendfileThreadCount(sendfileThreadCount); }
    public int getSendfileThreadCount() { return ((AprEndpoint)getEndpoint()).getSendfileThreadCount(); }

    public boolean getDeferAccept() { return ((AprEndpoint)getEndpoint()).getDeferAccept(); }
    public void setDeferAccept(boolean deferAccept) { ((AprEndpoint)getEndpoint()).setDeferAccept(deferAccept); }


    // ----------------------------------------------------- JMX related methods

    @Override
    protected String getNamePrefix() {
        if (isSSLEnabled()) {
            return ("https-apr");
        } else {
            return ("http-apr");
        }
    }


    // --------------------  Connection handler --------------------

    protected static class Http11ConnectionHandler
            extends AbstractHttp11ConnectionHandler<Long> {

        Http11ConnectionHandler(Http11AprProtocol proto) {
            super(proto);
        }

        @Override
        protected Log getLog() {
            return log;
        }

        @Override
        public void release(SocketWrapperBase<Long> socket,
                Processor processor, boolean addToPoller) {
            processor.recycle();
            recycledProcessors.push(processor);
            if (addToPoller && getProtocol().getEndpoint().isRunning()) {
                socket.setReadTimeout(getProtocol().getEndpoint().getKeepAliveTimeout());
                socket.registerReadInterest();
            }
        }

        @Override
        protected void longPoll(SocketWrapperBase<Long> socket, Processor processor) {

            if (processor.isAsync()) {
                // Async
                socket.setAsync(true);
            } else {
                // Upgraded
                Poller p = ((AprEndpoint) getProtocol().getEndpoint()).getPoller();
                if (p == null) {
                    // Connector has been stopped
                    release(socket, processor, false);
                } else {
                    socket.registerReadInterest();
                }
            }
        }
    }
}
