/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.tempalarm.backend;

import java.util.logging.Logger;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class SettingUtils {

    private static final Logger log = Logger.getLogger(SettingUtils.class.getSimpleName());

    public static SettingRecord getSettingRecord(final String name, final String defValue) {
        SettingRecord setting = ofy().load().type(SettingRecord.class).id(name).now();
        if (setting == null) {
            setting = new SettingRecord();
            setting.setName(name);
            setting.setValue(defValue);
            ofy().save().entity(setting).now();

            log.severe("Created setting " + name + " with default value! Please go to " +
                    "App Engine admin console to change its value.");
        }
        return setting;
    }

    public static String getSettingValue(final String name, final String defValue) {
        return getSettingRecord(name, defValue).getValue();
    }
}
