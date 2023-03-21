/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.communication.engine.dao;

import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WhatsappUsersReadBase extends ReadBase<WhatsappUsers> {
    public static final String ID = "id";
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String STATUS = "status";
    public static final String OPT_OUT_CONSENT_SENT = "opt_out_consent_sent";
    public static final String NAMESPACE = "namespace";
    public static final String SOURCE = "source";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<WhatsappUsers> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<WhatsappUsers> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("WhatsappUsers ResultSet is not initialized.");
            throw new SQLException("WhatsappUsers ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<WhatsappUsers>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateWhatsappUsers());
        }
        return records;
    }

    private WhatsappUsers populateWhatsappUsers() {
        WhatsappUsers whatsappUsers = null;
        try {
            WhatsappUsers.WhatsappUsersBuilder builder = WhatsappUsers.builder();
            builder.id(rs.getLong(ID));
            builder.mobileNumber(rs.getString(MOBILE_NUMBER));
            builder.status(WhatsappUsers.WhatsAppStatus.valueOf(rs.getString(STATUS)));
            builder.optOutConsentSent(rs.getBoolean(OPT_OUT_CONSENT_SENT));
            builder.namespace(rs.getString(NAMESPACE));
            builder.source(rs.getString(SOURCE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            whatsappUsers = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_WHATSAPP_USERS_READ_BASE, "populate_record_failed");
            log.error("Unable to populate WhatsappUsers from resultSet : {}", rs, e);
        }
        return whatsappUsers;
    }

    @Override
    protected WhatsappUsers getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("WhatsApp Users ResultSet is not initialized.");
            throw new SQLException("WhatsApp Users ResultSet is not initialized.");
        }

        WhatsappUsers whatsappUsers = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            whatsappUsers = populateWhatsappUsers();
        }
        return whatsappUsers;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
