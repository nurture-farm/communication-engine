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

package farm.nurture.communication.engine.service;

import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.util.http.AsyncHttpClientFactory;
import farm.nurture.util.http.HttpClientConfig;
import farm.nurture.util.http.HttpClientFactory;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import farm.nurture.util.http.client.NFHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

public abstract class BaseServiceTest {

    public NFAsyncHttpClient buildAsyncHttpClient() {
        HttpClientConfig config = createAsyncHttpClientConfig();
        CloseableHttpAsyncClient httpClient = new AsyncHttpClientFactory(config).createClient();
        return new NFAsyncHttpClient(httpClient);
    }

    public NFHttpClient buildHttpClient() {
        HttpClientConfig config = createHttpClientConfig();
        HttpClient httpClient = new HttpClientFactory(config).createClient();
        return new NFHttpClient(httpClient);
    }

    private HttpClientConfig createAsyncHttpClientConfig() {
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        HttpClientConfig config = new HttpClientConfig();

        config.setMaxTotalConnections(configuration.getInt("httpClientConfig.maxConnections", 150));
        config.setMaxConnectionsPerRoute(configuration.getInt("httpClientConfig.maxConnectionsPerRoute", 50));
        config.setConnectionTimeout(configuration.getInt("httpClientConfig.connectionTimeout", 7000));
        config.setSoTimeout(configuration.getInt("httpClientConfig.requestTimeout", 60000));
        config.setSoReuseAddress(configuration.getBoolean("httpClientConfig.soReuseAddress", true));
        config.setSoLinger(configuration.getInt("httpClientConfig.soLinger", 0));
        config.setSoKeepAlive(configuration.getBoolean("httpClientConfig.keepAlive", false));
        config.setTcpNoDelay(configuration.getBoolean("httpClientConfig.tcpNoDelay", false));
        return config;
    }

    private HttpClientConfig createHttpClientConfig() {
        ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
        HttpClientConfig config = new HttpClientConfig();

        config.setMaxTotalConnections(configuration.getInt("httpClientConfig.maxConnections", 150));
        config.setMaxConnectionsPerRoute(configuration.getInt("httpClientConfig.maxConnectionsPerHost", 50));
        config.setConnectionTimeout(configuration.getInt("httpClientConfig.connectionTimeout", 7000));
        config.setSoTimeout(configuration.getInt("httpClientConfig.requestTimeout", 60000));
        config.setSoReuseAddress(configuration.getBoolean("httpClientConfig.soReuseAddress", true));
        config.setSoLinger(configuration.getInt("httpClientConfig.soLinger", 0));
        config.setSoKeepAlive(configuration.getBoolean("httpClientConfig.keepAlive", false));
        config.setTcpNoDelay(configuration.getBoolean("httpClientConfig.tcpNoDelay", false));
        return config;
    }
}
