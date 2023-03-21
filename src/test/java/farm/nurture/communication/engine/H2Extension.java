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

package farm.nurture.communication.engine;

import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.laminar.core.io.sql.dao.DbConfig;
import farm.nurture.laminar.core.io.sql.dao.PoolFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class H2Extension implements BeforeAllCallback, AfterAllCallback {
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        log.info("Initializing database");
        ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
        DbConfig dbConfig = new DbConfig();
        dbConfig.connectionUrl = appConfig.get("db.connection.url");
        dbConfig.login = appConfig.get("db.username");
        dbConfig.password = appConfig.get("db.password");
        dbConfig.driverClass = appConfig.get("db.driver.class", "org.h2.Driver");
        dbConfig.poolName = appConfig.get("db.connection.pool.name", "communication_rw");
        dbConfig.idleConnections = appConfig.getInt("db.idle.connections", 2);
        dbConfig.maxConnections = appConfig.getInt("db.max.connections", 10);
        dbConfig.incrementBy = appConfig.getInt("db.connection.increment.by", 2);
        dbConfig.testConnectionOnBorrow = true;
        PoolFactory.getInstance().setup(dbConfig);
    }
}
