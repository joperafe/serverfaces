/*
 * Copyright 2012 Rafael M. Pestano.  
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.serverfaces.agent.mib;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.serverfaces.agent.event.InitMibEvent;
import org.serverfaces.agent.event.UnregisterMOsEvent;
import org.serverfaces.agent.event.UpdateMibEvent;
import org.serverfaces.agent.qualifier.Oid;
import org.serverfaces.agent.server.ServerRetriever;
import org.serverfaces.agent.util.MOCreator;
import org.serverfaces.common.qualifier.Log;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 *
 * @author Rafael M. Pestano - Nov 02, 2012 07:17:18 AM
 *
 * The MIB manager is the guy who asks the current server retriever for valuable
 * information to then register/update this information as managed objects in
 * agent mib(MOGroup)
 */
@ApplicationScoped
public class MibManager implements Serializable, MOGroup {

    @Inject
    @Log
    Logger log;
    private List<ManagedObject> objects = new LinkedList<ManagedObject>();
    private DefaultMOServer moServer;
    private OctetString context;
    @Inject
    Instance<OID> serverName;
    @Inject
    Instance<OID> serverUptime;
    @Inject
    Instance<OID> serverActiveSessions;
    @Inject
    Instance<OID> serverUsedMemory;
    @Inject
    Instance<OID> serverAvailableMemory;
    @Inject
    Instance<OID> serverCpuTime;
    @Inject
    Instance<OID> serverActiveTransactions;
    @Inject
    Instance<OID> serverCommitedTransactions;
    @Inject
    Instance<OID> serverRollbackTransactions;
    @Inject
    Instance<OID> serverActiveThreads;
    @Inject
    Instance<OID> serverTotalRequests;
    @Inject
    Instance<OID> serverAddress;
    @Inject
    Instance<OID> serverLog;
    
    @Inject Event<UnregisterMOsEvent> unregisterMOsEvent;
    
    /**
     * A server retriever is the guy who retrieves information from a server,
     * the default retriever is GlassfishRetriever you can switch the retriever
     * via CDI Alternatives see:
     * http://docs.oracle.com/javaee/6/tutorial/doc/gjsdf.html
     */
    @Inject
    ServerRetriever serverRetriever;

  

    /**
     * 
     * initializes mib with MOServer and context and
     * register known OIDs in the management information base
     */
    public void initMIB(@Observes InitMibEvent initMibEvent) {
            
            this.moServer = initMibEvent.getDefaultMOServer();
            this.context = initMibEvent.getDefaultContext();
            this.initMOs();
    }
    
     /**
     * 
     * initializes mib with MOServer and context and
     * register known OIDs in the management information base
     */
    public void initMIB(DefaultMOServer moServer,OctetString context) {
            
            this.moServer = moServer;
            this.context = context;
            this.initMOs();
    }

    /**
     * register known OIDs in the management information base
     * 
     */
    private void initMOs() {
        log.debug("Registering Mib objects...");
         try {
            // register MOs
            addInstance(MOCreator.createReadWrite(serverName.get(),
                    serverRetriever.getServerName()));
            addInstance(MOCreator.createReadWrite(serverAddress.get(),
                    serverRetriever.getServerAddress()));
            addInstance(MOCreator.createReadWrite(serverUptime.get(),
                    serverRetriever.getServerUpTime()));
            addInstance(MOCreator.createReadWrite(serverActiveSessions.get(),
                    serverRetriever.getServerActiveSessions()));
            addInstance(MOCreator.createReadWrite(serverUsedMemory.get(),
                    serverRetriever.getServerUsedMemory()));
            addInstance(MOCreator.createReadWrite(serverAvailableMemory.get(),
                    serverRetriever.getServerAvailableMemory()));
            addInstance(MOCreator.createReadWrite(serverCpuTime.get(),
                    serverRetriever.getServerCpuTime()));
            addInstance(MOCreator.createReadWrite(serverActiveTransactions.get(),
                    serverRetriever.getServerActiveTransactions()));
            addInstance(MOCreator.createReadWrite(serverCommitedTransactions.get(),
                    serverRetriever.getServerCommitedTransactions()));
            addInstance(MOCreator.createReadWrite(serverRollbackTransactions.get(),
                    serverRetriever.getServerRollbackTransactions()));
            addInstance(MOCreator.createReadWrite(serverActiveThreads.get(),
                    serverRetriever.getServerActiveThreads()));
            addInstance(MOCreator.createReadWrite(serverTotalRequests.get(),
                    serverRetriever.getServerTotalRequests()));
            addInstance(MOCreator.createReadWrite(serverLog.get(),
                    serverRetriever.getServerLog()));
            this.registerMOs();
        } catch (DuplicateRegistrationException ex) {
            log.debug("trying to register duplicate object");
            if(log.isDebugEnabled()){
                  ex.printStackTrace();
            }
        }
        log.debug("MIB objects registered successfully");
    }
    
    @Override
    public void registerMOs(MOServer server, OctetString context)
            throws DuplicateRegistrationException {
        for (ManagedObject mo : objects) {
            server.register(mo, context);
        }
    }

    public DefaultMOServer getMoServer() {
        return moServer;
    }

    public void setMoServer(DefaultMOServer moServer) {
        this.moServer = moServer;
    }

    public void registerMOs() throws DuplicateRegistrationException  {
            this.registerMOs(moServer, context);
        }

    public void unregisterMOs() {
        this.unregisterMOs(moServer, context);
    }
    
    

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        for (ManagedObject mo : objects) {
            server.unregister(mo, context);
        }
    }

    public boolean addInstance(ManagedObject mo) {
        return objects.add(mo);
    }

    public boolean removeInstance(ManagedObject mo) {
        return objects.remove(mo);
    }

    public OctetString getContext() {
        return context;
    }

    public void setContext(OctetString context) {
        this.context = context;
    }

    @PreDestroy
    public void terminateMIB() {
        unregisterMOsEvent.fire(new UnregisterMOsEvent(this));
    }

    public void updateMIB(@Observes UpdateMibEvent updateMib) {
        this.updateMIB();
    }

    /**
     * updates dynamic values in management information base
     */
    public void updateMIB() {
        OctetString uptime = new OctetString();
        uptime.setValue(serverRetriever.getServerUpTime());
        this.setScalar(getServerUptime(), uptime);
        this.setScalar(getServerActiveSessions(), new OctetString(serverRetriever.getServerActiveSessions()));
        this.setScalar(getServerUsedMemory(), new OctetString(serverRetriever.getServerUsedMemory()));
        this.setScalar(getServerAvailableMemory(), new OctetString(serverRetriever.getServerAvailableMemory()));
        this.setScalar(getServerCpuTime(), new OctetString(serverRetriever.getServerCpuTime()));
        this.setScalar(getServerActiveTransactions(), new OctetString(serverRetriever.getServerActiveTransactions()));
        this.setScalar(getServerCommitedTransactions(), new OctetString(serverRetriever.getServerCommitedTransactions()));
        this.setScalar(getServerRollbackTransactions(), new OctetString(serverRetriever.getServerRollbackTransactions()));
        this.setScalar(getServerActiveThreads(), new OctetString(serverRetriever.getServerActiveThreads()));
        this.setScalar(getServerTotalRequests(), new OctetString(serverRetriever.getServerTotalRequests()));
//        this.setScalar(serverLog.get(), new OctetString(serverRetriever.getServerLog()));
    }

    public MOScalar findScalar(OID oid) {
        return (MOScalar) getMoServer().getManagedObject(oid, getContext());
    }

    public void setScalar(OID oid, Variable value) {
        findScalar(oid).setValue(value);
    }

    public ServerRetriever getServerRetriever() {
        return serverRetriever;
    }

    @Produces
    public MOScalar produceMOScalar(InjectionPoint ip) {
        if (ip.getAnnotated().isAnnotationPresent(Oid.class)) {
            String oidString = ip.getAnnotated().getAnnotation(Oid.class).value();
            return (MOScalar) getMoServer().getManagedObject(new OID(oidString), getContext());
        }
        return null;
    }
    
       public OID getServerName() {
        return serverName.get();
    }

    public OID getServerUptime() {
        return serverUptime.get();
    }

    public OID getServerActiveSessions() {
        return serverActiveSessions.get();
    }

    public OID getServerUsedMemory() {
        return serverUsedMemory.get();
    }

    public OID getServerAvailableMemory() {
        return serverAvailableMemory.get();
    }

    public OID getServerCpuTime() {
        return serverCpuTime.get();
    }

    public OID getServerActiveTransactions() {
        return serverActiveTransactions.get();
    }

    public OID getServerCommitedTransactions() {
        return serverCommitedTransactions.get();
    }

    public OID getServerRollbackTransactions() {
        return serverRollbackTransactions.get();
    }

    public OID getServerActiveThreads() {
        return serverActiveThreads.get();
    }

    public OID getServerTotalRequests() {
        return serverTotalRequests.get();
    }

    public OID getServerAddress() {
        return serverAddress.get();
    }

    public OID getServerLog() {
        return serverLog.get();
    }

   
}
