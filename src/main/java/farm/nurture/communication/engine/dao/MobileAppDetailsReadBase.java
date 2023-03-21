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

import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.MobileAppDetails.MobileAppDetailsBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MobileAppDetailsReadBase extends ReadBase<MobileAppDetails> {

    public static final String ID = "id";
    public static final String APP_ID = "app_id";
    public static final String APP_NAME = "app_name";
    public static final String APP_TYPE = "app_type";
    public static final String FCM_API_KEY = "fcm_api_key";
    public static final String AFS_APP_ID = "afs_app_id";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<MobileAppDetails> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<MobileAppDetails> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("MobileAppDetails ResultSet is not initialized.");
            throw new SQLException("MobileAppDetails ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<MobileAppDetails>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateMobileAppDetails());
        }
        return records;
    }

    private MobileAppDetails populateMobileAppDetails() {
        MobileAppDetails appDetails = null;
        try {
            MobileAppDetailsBuilder builder = MobileAppDetails.builder();
            builder.id(rs.getShort(ID));
            builder.appId(rs.getString(APP_ID));
            builder.appName(rs.getString(APP_NAME));
            builder.appType(MobileAppDetails.AppType.valueOf(rs.getString(APP_TYPE)));
            builder.fcmApiKey(rs.getString(FCM_API_KEY));
            builder.afsAppId(rs.getShort(AFS_APP_ID));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            appDetails = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_MOBILE_APP_DETAILS_READ_BASE, "populate_record_failed");
            log.error("Unable to populate MobileAppDetails from resultSet : {}", rs, e);
        }
        return appDetails;
    }

    @Override
    protected MobileAppDetails getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("MobileAppDetails ResultSet is not initialized.");
            throw new SQLException("MobileAppDetails ResultSet is not initialized.");
        }

        MobileAppDetails appDetails = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            appDetails = populateMobileAppDetails();
        }
        return appDetails;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
