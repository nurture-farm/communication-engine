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

package farm.nurture.communication.engine.utils;

import com.google.inject.Singleton;
import farm.nurture.communication.engine.vendor.VendorType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Singleton
public class VendorLoadBalancer {

    private static Map<String, Map<String, Integer>> channelToVendorWeightMap = new HashMap<>();

    private static Map<String, List<String>> channelToVendorList = new HashMap<>();

    public void initializeVendorWeightedMap(String communicationChannel, Map<String, Integer> vendorToWeightMap){
        channelToVendorWeightMap.put(communicationChannel, vendorToWeightMap);
    }

    public void init(){
        channelToVendorWeightMap.forEach((channel, vendorToWeightMap) ->{
            List<String> vendorList = new ArrayList<>();
            vendorToWeightMap.forEach((vendor, weight) ->{
               int weightX = weight;
               while(weightX-- > 0){
                   vendorList.add(vendor);
               }
            });
            channelToVendorList.put(channel, vendorList);
        });
    }

    public VendorType getVendor(String communicationChannel){
        List<String> vendorList = channelToVendorList.get(communicationChannel);
        int index = new Random().nextInt(vendorList.size());
        return VendorType.valueOf(vendorList.get(index));
    }

}