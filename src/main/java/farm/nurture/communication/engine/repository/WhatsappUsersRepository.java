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
import farm.nurture.communication.engine.dao.WhatsappUsersReadBase;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@Singleton
public class WhatsappUsersRepository {
    private final Metrics metrics = Metrics.getInstance();

    private static final String getByMobileNumberSql = "SELECT * FROM whatsapp_users WHERE mobile_number = ?";

    private static final String insertWhatsappUsersSql = "INSERT INTO whatsapp_users(mobile_number, status, opt_out_consent_sent, namespace, source) " +
            "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = ?, opt_out_consent_sent = ?";

    private static final String updateWhatsappUsersSql = "UPDATE whatsapp_users set deleted_at = NULL, status = ? " +
            "where mobile_number = ?";

    private static final String updateWhatsappUsersOptOutConsentSentSql = "UPDATE whatsapp_users set deleted_at = NULL, opt_out_consent_sent = ? " +
            "where mobile_number = ?";

    private static final String updateWhatsappUsersUpdatedTime=  "UPDATE whatsapp_users set updated_at = now() " +
            "where mobile_number = ?";

    private static final String deleteUpdateWhatsappUsersSql = "UPDATE whatsapp_users set deleted_at = now() " +
            "where mobile_number = ?";

    public WhatsappUsers getByMobileNumberKey(String mobileNumber) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "select_whatsapp_users");
        WhatsappUsersReadBase readBase = new WhatsappUsersReadBase();
        WhatsappUsers whatsappUsers = null;
        try {
            Object[] arrayOfKeys = {mobileNumber};
            whatsappUsers = readBase.selectByUniqueKey(getByMobileNumberSql, arrayOfKeys);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching WhatsappUsers for mobileNumber : {}", mobileNumber, e);
        } finally {
            tracker.stop(success);
        }
        return whatsappUsers;
    }

    public boolean insertWhatsappUsers(WhatsappUsers whatsappUsers) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_whatsapp_users");
        try {
            WriteBase writeBase = new WriteBase();
            int recordId = writeBase.insert(insertWhatsappUsersSql, Arrays.asList(whatsappUsers.getMobileNumber(), whatsappUsers.getStatus().name(),
                    whatsappUsers.getOptOutConsentSent(), whatsappUsers.getNamespace(), whatsappUsers.getSource(),
                    whatsappUsers.getStatus().name(), whatsappUsers.getOptOutConsentSent()));
            whatsappUsers.setId(new Long(recordId));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting WhatsappUsers : {}", whatsappUsers, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean findAndUpdateWhatsappUsers(WhatsappUsers whatsappUsers) {
        WhatsappUsers users = getByMobileNumberKey(whatsappUsers.getMobileNumber());
        if(users == null) {
            return insertWhatsappUsers(whatsappUsers);
        }

        return updateWhatsappUsers(whatsappUsers);
    }

    public boolean updateWhatsappUsers(WhatsappUsers whatsappUsers) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_whatsapp_users");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(updateWhatsappUsersSql, Arrays.asList(whatsappUsers.getStatus().name(), whatsappUsers.getMobileNumber()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating WhatsappUsers : {}", whatsappUsers, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }
    public boolean updateWhatsappUsersUpdatedTime(WhatsappUsers whatsappUsers) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_whatsapp_users");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(updateWhatsappUsersUpdatedTime, Arrays.asList(whatsappUsers.getMobileNumber()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating WhatsappUsers : {}", whatsappUsers, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }
    public boolean updateWhatsappUsersOptOutConsentSent(WhatsappUsers whatsappUsers) {
        WhatsappUsers users = getByMobileNumberKey(whatsappUsers.getMobileNumber());
        if(users == null) {
            return insertWhatsappUsers(whatsappUsers);
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_whatsapp_users_opt_out_consent_sent");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(updateWhatsappUsersOptOutConsentSentSql,
                    Arrays.asList(whatsappUsers.getOptOutConsentSent(), whatsappUsers.getMobileNumber()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating Whatsapp Users Opt Out Consent Sent using whatsappUsers: {}, for existing user: {}", whatsappUsers, users, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }

    public boolean deleteWhatsappUsers(WhatsappUsers whatsappUsers) {
        WhatsappUsers users = getByMobileNumberKey(whatsappUsers.getMobileNumber());
        if(users == null) {
            return false;
        }

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "delete_whatsapp_users");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(deleteUpdateWhatsappUsersSql, Arrays.asList(whatsappUsers.getMobileNumber()));
            success = true;

        } catch (Exception e) {
            log.error("Error in updating WhatsappUsers : {}, using existing user: {}", whatsappUsers, users, e);

        } finally {
            tracker.stop(success);
        }
        return success;
    }
}
