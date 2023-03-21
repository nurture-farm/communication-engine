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

package farm.nurture.communication.engine.metric;

public class MetricTracker {

    private static final Metrics metrics = Metrics.getInstance();

    private String group;
    private String name;
    private long startTime;
    private String[] labels;

    public MetricTracker(String group, String name,String... labels) {
        this.group = group;
        this.name = name;
        this.startTime = System.currentTimeMillis();
        this.labels = labels;
    }

    public void stop(Boolean success, String... labelValues) {
        metrics.getIMetricCounter(this.group, this.name, success, this.labels).increment(labelValues);
        metrics.getIMetricSummary(this.group, this.name, this.labels).observe(System.currentTimeMillis() - this.startTime, labelValues);
    }
}
