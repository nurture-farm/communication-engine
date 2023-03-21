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

import farm.nurture.infra.metrics.IMetricCounter;
import farm.nurture.infra.metrics.IMetricSummary;
import farm.nurture.infra.metrics.MetricFactory;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Metrics {

    static Metrics metrics = null;

    private Map<String, IMetricCounter> metricCounters;
    private Map<String, IMetricSummary> metricSummary;


    public static final String SUCCESS = "requests_success";
    public static final String FAILED = "requests_failed";
    public static final String SUMMARY_MS = "requests_summary_duration_ms";

    private Metrics() {
        metricCounters = new ConcurrentHashMap<String, IMetricCounter>();
        metricSummary = new ConcurrentHashMap<String, IMetricSummary>();
    }

    public static Metrics getInstance() {
        if(metrics == null) {
            synchronized (Metrics.class) {
                if(metrics == null) {
                    metrics = new Metrics();
                }
            }
        }

        return metrics;
    }

    public IMetricCounter getIMetricCounter(String group, String name) {

        IMetricCounter t = metricCounters.get(group + "_" + name);
        if (null == t) {
            synchronized (metricCounters) {
                t = metricCounters.get(group + "_" + name);
                if (t == null) {
                    t = MetricFactory.getCounter(group, name).register();
                    metricCounters.put(group + "_" + name, t);
                }
            }
        }
        return t;
    }

    public IMetricCounter getIMetricCounter(String group, String name, String... labels) {
        StringBuilder labelStr = new StringBuilder();
        if(labels != null) {
            for (int i = 0; i < labels.length; i++) {
                labelStr.append("_" + labels[i]);
            }
        }

        String key = group + "_" + name + labelStr.toString();
        IMetricCounter t = metricCounters.get(key);
        if (null == t) {
            synchronized (metricCounters) {
                t = metricCounters.get(key);
                if (t == null) {
                    t = MetricFactory.getCounter(group, name, labels).register();
                    metricCounters.put(key, t);
                }
            }
        }
        return t;
    }

    public IMetricCounter getIMetricCounter(String group, String name, Boolean success, String... labels) {
        return success ? getIMetricCounter(group, name + "_" + SUCCESS, labels) : getIMetricCounter(group, name + "_" + FAILED, labels);
    }

    public IMetricSummary getIMetricSummary(String group, String name) {
        group = format(group);
        name = format(name);
        String metricName = group + "_" + name + "_" + SUMMARY_MS;
        IMetricSummary t = metricSummary.get(metricName);
        if (null == t) {
            synchronized (metricSummary) {
                t = metricSummary.get(metricName);
                if (t == null) {
                    t = MetricFactory.getSummary(group, name + "_" + SUMMARY_MS).register();
                    metricSummary.put(metricName, t);
                }
            }
        }
        return t;
    }

    public IMetricSummary getIMetricSummary(String group, String name, String... labels) {
        StringBuilder labelStr = new StringBuilder();
        if(labels != null) {
            for (int i = 0; i < labels.length; i++) {
                labelStr.append("_" + labels[i]);
            }
        }

        group = format(group);
        name = format(name);
        String metricName = group + "_" + name + labelStr.toString() + "_" + SUMMARY_MS;
        IMetricSummary t = metricSummary.get(metricName);
        if (null == t) {
            synchronized (metricSummary) {
                t = metricSummary.get(metricName);
                if (t == null) {
                    t = MetricFactory.getSummary(group, name + "_" + SUMMARY_MS, labels).register();
                    metricSummary.put(metricName, t);
                }
            }
        }
        return t;
    }

    public void onIncrement(String group, String name) {
        try {
            group = format(group);
            name = format(name);
            getIMetricCounter(group, name).increment();
        } catch (Exception e) {
            log.error("Ignoring Metric Exception", e);
        }
    }

    public void onIncrement(String group, String name, String[] labels,String... labelValues) {
        try {
            group = format(group);
            name = format(name);
            getIMetricCounter(group, name, labels).increment(labelValues);
        } catch (Exception e) {
            log.error("Exception in counter metrics. Group: {}, Name: {}, Labels: {}, LabelValues: {}", group, name, labels, labelValues, e);
        }
    }

    public void onIncrementBy(String group, String name, int incrementBy) {
        try {
            group = format(group);
            name = format(name);
            getIMetricCounter(group, name).increment(incrementBy);
        } catch (Exception e) {
            log.error("Ignoring Metric Exception - onIncrementBy", e);
        }
    }

    public String format(String name) {
        if (StringUtils.isEmpty(name)) {
            return "null";
        }
        return name.trim().replaceAll("[^a-zA-Z0-9_:]", "_");
    }
}
