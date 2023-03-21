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

package farm.nurture.communication.engine.repository;

import com.google.inject.Singleton;
import farm.nurture.communication.engine.dao.MobileAppDetailsReadBase;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Singleton
public class MobileAppDetailsRepository {

    private static final String getAllSql = "SELECT * FROM mobile_app_details";

    private static final String getMobileAppDetailsByIdSql = "SELECT * FROM mobile_app_details WHERE id = ?";

    private static final String getMobileAppDetailsByAFSAppIdSql = "SELECT * FROM mobile_app_details WHERE afs_app_id = ?";

    private static final String getMobileAppDetailsByAppIdandAppNameSql = "SELECT * FROM mobile_app_details WHERE app_id = ? and app_type = ?";

    private static final String insertMobileAppDetailsSql = "INSERT INTO mobile_app_details(app_id, app_name, app_type, fcm_api_key, afs_app_id) " +
            "VALUES(?, ?, ?, ?, ?)";

    public List<MobileAppDetails> getAll() {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_all_mobile_app_details");
        MobileAppDetailsReadBase readBase = new MobileAppDetailsReadBase();
        List<MobileAppDetails> mobileAppDetails = null;
        try {
            mobileAppDetails = readBase.execute(getAllSql);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching all MobileAppDetails", e);

        } finally {
            tracker.stop(success);
        }
        return mobileAppDetails;
    }

    public void insertMobileAppDetails(MobileAppDetails appDetails) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_mobile_app_details");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(insertMobileAppDetailsSql, Arrays.asList(appDetails.getAppId(), appDetails.getAppName(), appDetails.getAppType().name(),
                    appDetails.getFcmApiKey(), appDetails.getAfsAppId()));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting MobileAppDetails : {}", appDetails, e);
        } finally {
            tracker.stop(success);
        }
    }

    public MobileAppDetails getMobileAppDetailsById(Short id) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_mobile_app_details_by_id");
        MobileAppDetailsReadBase readBase = new MobileAppDetailsReadBase();
        MobileAppDetails appDetails = null;
        try {
            appDetails = readBase.selectByPrimaryKey(getMobileAppDetailsByIdSql, id);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MobileAppDetails by id : {}", id, e);

        } finally {
            tracker.stop(success);
        }
        return appDetails;
    }

    public MobileAppDetails getMobileAppDetailsByAFSAppId(Short afsAppId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_mobile_app_details_by_afs_app_id");
        MobileAppDetailsReadBase readBase = new MobileAppDetailsReadBase();
        MobileAppDetails appDetails = null;
        try {
            appDetails = readBase.selectByUniqueKey(getMobileAppDetailsByAFSAppIdSql, Arrays.asList(afsAppId).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MobileAppDetails by afsAppId : {}", afsAppId, e);

        } finally {
            tracker.stop(success);
        }
        return appDetails;
    }

    public MobileAppDetails getMobileAppDetailsByAppIdandAppName(String appId, String appType) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_mobile_app_details_by_app_id_and_app_type");
        MobileAppDetailsReadBase readBase = new MobileAppDetailsReadBase();
        MobileAppDetails appDetails = null;
        try {
            appDetails = readBase.selectByUniqueKey(getMobileAppDetailsByAppIdandAppNameSql, Arrays.asList(appId, appType).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching MobileAppDetails by AppId : {} and AppType : {}", appId, appType, e);

        } finally {
            tracker.stop(success);
        }
        return appDetails;
    }
}
