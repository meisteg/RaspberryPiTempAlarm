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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class AlertEmail {

    private static final Logger log = Logger.getLogger(AlertEmail.class.getSimpleName());

    private static final String SUBJECT_LOW_TEMP = "URGENT: Low temperature detected!";
    private static final String SUBJECT_NORM_TEMP = "FYI: Temperature has returned to allowed range";
    private static final String SUBJECT_PWR_OUT = "URGENT: Possible power outage!";
    private static final String SUBJECT_RUNNING = "FYI: Temperature monitoring is running";
    private static final String SUBJECT_STOPPED = "FYI: Temperature monitoring has been stopped";

    private static final String BODY_TEMP_TEMPLATE = "Temperature is %s degrees.";
    private static final String BODY_PWR_OUT = "No longer receiving data from sensor.";
    private static final String BODY_STOPPED = "If this is unexpected, please check the sensor.";

    private static final String FOOTER = "\n\nhttp://rasptempalarm.appspot.com";

    public static boolean sendLowTemp(final String temp) {
        return sendToAll(SUBJECT_LOW_TEMP, String.format(BODY_TEMP_TEMPLATE, temp));
    }

    public static boolean sendNormTemp(final String temp) {
        return sendToAll(SUBJECT_NORM_TEMP, String.format(BODY_TEMP_TEMPLATE, temp));
    }

    public static boolean sendPwrOut() {
        return sendToAll(SUBJECT_PWR_OUT, BODY_PWR_OUT);
    }

    public static boolean sendRunning(final String temp) {
        return sendToAll(SUBJECT_RUNNING, String.format(BODY_TEMP_TEMPLATE, temp));
    }

    public static boolean sendStopped() {
        return sendToAll(SUBJECT_STOPPED, BODY_STOPPED);
    }

    public static boolean sendToAll(final String subject, final String body) {
        return sendTo(subject, body, getRecipients());
    }

    public static boolean sendToAdmins(final String subject, final String body) {
        try {
            return sendTo(subject, body, new InternetAddress("admins"));
        } catch (final AddressException e) {
            log.severe("Failed to send message: " + e);
            return false;
        }
    }

    private static boolean sendTo(final String subject, final String body,
                                  final InternetAddress... recipients) {
        if ((subject == null) || (body == null) || (recipients == null)) {
            log.warning("Parameters to sendTo cannot be null.");
            return false;
        }

        if (recipients.length == 0) {
            log.warning("No recipients specified.");
            return false;
        }

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
        final List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).list();
        for (final RegistrationRecord record : records) {
            try {
                recipients.add(new InternetAddress(record.getUserEmail()));
            } catch (final AddressException e) {
                log.severe("Failed to add " + record.getUserEmail() + " as recipient");
            }
        }

        final String extraEmails = SettingUtils.getSettingValue(Constants.SETTING_EXTRA_EMAILS,
                Constants.DEFAULT_EXTRA_EMAILS);
        if (extraEmails != null) {
            for (final String extraEmail : extraEmails.split(",")) {
                try {
                    recipients.add(new InternetAddress(extraEmail));
                } catch (final AddressException e) {
                    log.severe("Failed to add " + extraEmail + " as recipient");
                }
            }
        }

        return recipients.toArray(new InternetAddress[recipients.size()]);
    }
}
