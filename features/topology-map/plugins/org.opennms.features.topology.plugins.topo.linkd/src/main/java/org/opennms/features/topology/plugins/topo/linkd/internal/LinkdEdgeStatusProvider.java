/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.plugins.topo.linkd.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opennms.core.criteria.restrictions.EqRestriction;
import org.opennms.core.criteria.restrictions.NeRestriction;
import org.opennms.features.topology.api.topo.Criteria;
import org.opennms.features.topology.api.topo.EdgeRef;
import org.opennms.features.topology.api.topo.EdgeStatusProvider;
import org.opennms.features.topology.api.topo.Status;
import org.opennms.features.topology.api.topo.BackendGraph;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.dao.api.SessionUtils;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsSeverity;
import org.opennms.netmgt.topologies.service.api.OnmsTopology;

import com.google.common.collect.Maps;

public class LinkdEdgeStatusProvider implements EdgeStatusProvider {

    public static class LinkdEdgeStatus implements Status {

        private final String m_status;

        public LinkdEdgeStatus(String status) {
            m_status = status;
        }

        public LinkdEdgeStatus(OnmsAlarm summary) {
            m_status = summary.getUei().equals(EventConstants.TOPOLOGY_LINK_DOWN_EVENT_UEI) ? "down" : "up";
        }

        @Override
        public String computeStatus() {
            return m_status.toLowerCase();
        }

        @Override
        public Map<String, String> getStatusProperties() {
            Map<String, String> statusMap = new LinkedHashMap<String, String>();
            statusMap.put("status", m_status);

            return statusMap;
        }

        @Override
        public Map<String, String> getStyleProperties() {
            return Maps.newHashMap();
        }

        @Override
        public String toString() {
            return "LinkdEdgeStatus[" + m_status + "]";
        }
    }

    private AlarmDao m_alarmDao;
    private SessionUtils m_sessionUtils;

    @Override
    public String getNamespace() {
        return OnmsTopology.TOPOLOGY_NAMESPACE_LINKD;
    }

    @Override
    public Map<EdgeRef, Status> getStatusForEdges(BackendGraph graph, Collection<EdgeRef> edges, Criteria[] criteria) {
        Map<EdgeRef, Status> retVal = new LinkedHashMap<EdgeRef, Status>();
EDGES:        for (EdgeRef edgeRef : edges) {
                LinkdEdge edge = (LinkdEdge) graph.getEdge(edgeRef);
                for (OnmsAlarm alarm: getLinkdEdgeDownAlarms()) {
                    if (alarm.getNode() == null)
                        continue;
                    if (alarm.getIfIndex() == null)
                        continue;
                    int alarmnodeid = alarm.getNode().getId();
                    if ( edge.getSourcePort().getVertex().getNodeID() != null 
                            && edge.getSourcePort().getVertex().getNodeID() == alarmnodeid
                            && edge.getSourcePort().getIfIndex() != null
                            && edge.getSourcePort().getIfIndex().intValue() == alarm.getIfIndex().intValue()) {
                        retVal.put(edgeRef, new LinkdEdgeStatus(alarm));
                        continue EDGES;
                    }
                    if ( edge.getTargetPort().getVertex().getNodeID() != null 
                            && edge.getTargetPort().getVertex().getNodeID() == alarmnodeid
                            && edge.getTargetPort().getIfIndex() != null
                            && edge.getTargetPort().getIfIndex().intValue() == alarm.getIfIndex().intValue()) {
                        retVal.put(edgeRef, new LinkdEdgeStatus(alarm));
                        continue EDGES;
                    }                
              }
              retVal.put(edgeRef, new LinkdEdgeStatus("up"));
        }
        return retVal;
    }

    @Override
    public boolean contributesTo(String namespace) {
        return namespace.equals(OnmsTopology.TOPOLOGY_NAMESPACE_LINKD);
    }

    public AlarmDao getAlarmDao() {
        return m_alarmDao;
    }

    protected List<OnmsAlarm> getLinkdEdgeDownAlarms() {
        return getSessionUtils().withReadOnlyTransaction(() -> {
            org.opennms.core.criteria.Criteria criteria = new org.opennms.core.criteria.Criteria(OnmsAlarm.class);
            criteria.addRestriction(new EqRestriction("uei", EventConstants.TOPOLOGY_LINK_DOWN_EVENT_UEI));
            criteria.addRestriction(new NeRestriction("severity", OnmsSeverity.CLEARED));
            return getAlarmDao().findMatching(criteria);
        });
    }

    public void setAlarmDao(AlarmDao alarmDao) {
        m_alarmDao = alarmDao;
    }

    public SessionUtils getSessionUtils() {
        return m_sessionUtils;
    }

    public void setSessionUtils(SessionUtils m_sessionUtils) {
        this.m_sessionUtils = m_sessionUtils;
    }
}
