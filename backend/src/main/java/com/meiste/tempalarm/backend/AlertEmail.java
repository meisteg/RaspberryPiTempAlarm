/*
 * Copyright (C) 2014-2016 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class AlertEmail {

    private static final Logger log = Logger.getLogger(AlertEmail.class.getSimpleName());

    private static final String SUBJECT_HIGH_TEMP = "URGENT: High temperature detected!";
    private static final String SUBJECT_LOW_TEMP = "URGENT: Low temperature detected!";
    private static final String BODY_TEMP_TEMPLATE = "Temperature is %s degrees.";
    private static final String FOOTER = "\n\nhttp://rasptempalarm.appspot.com";

    public static boolean sendLowTemp(final String temp) {
        return send(SUBJECT_LOW_TEMP, temp);
    }

    public static boolean sendHighTemp(final String temp) {
        return send(SUBJECT_HIGH_TEMP, temp);
    }

    private static boolean send(final String subject, final String temp) {
        final InternetAddress[] recipients = getRecipients();
        if (recipients.length == 0) {
            log.warning("No recipients specified.");
            return false;
        }

        final String body = String.format(BODY_TEMP_TEMPLATE, temp);
        final Properties props = new Properties();
        final Session session = Session.getDefaultInstance(props, null);
        final Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress("greg.meiste@gmail.com", "Shop Temp Alarm"));
            msg.addRecipients(Message.RecipientType.BCC, recipients);
            msg.setSubject(subject);
            msg.setText(body + FOOTER);
            Transport.send(msg);
        } catch (final Exception e) {
            log.severe("Failed to send message: " + e);
            return false;
        }

        return true;
    }

    /**
     * Retrieves the recipient addresses from the datastore.
     *
     * @return Array of recipients
     */
    private static InternetAddress[] getRecipients() {
        final Set<InternetAddress> recipients = new HashSet<>();

        final String emails =
                SettingUtils.getSettingValue(Constants.SETTING_EMAILS, Constants.DEFAULT_EMAILS);
        if (emails != null) {
            for (final String addr : emails.split(",")) {
                try {
                    recipients.add(new InternetAddress(addr));
                } catch (final AddressException e) {
                    log.severe("Failed to add " + addr + " as recipient");
                }
            }
        }

        return recipients.toArray(new InternetAddress[recipients.size()]);
    }
}
