/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.util.notificationq;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import com.ning.billing.util.clock.Clock;
import com.ning.billing.util.queue.PersistentQueueEntryLifecycle.PersistentQueueEntryLifecycleState;

import com.google.inject.Inject;

public class MockNotificationQueueService extends NotificationQueueServiceBase {

    @Inject
    public MockNotificationQueueService(final Clock clock, final NotificationQueueConfig config) {
        super(clock, config, null, null);
    }


    @Override
    protected NotificationQueue createNotificationQueueInternal(final String svcName, final String queueName,
                                                                final NotificationQueueHandler handler) {
        return new MockNotificationQueue(clock, svcName, queueName, handler, this);
    }


    @Override
    public int doProcessEvents() {

        int retry = 2;
        do {
            try {
                int result = 0;
                Iterator<String> it = queues.keySet().iterator();
                while (it.hasNext()) {
                    final String queueName = it.next();
                    final NotificationQueue cur = queues.get(queueName);
                    if (cur != null) {
                        result += doProcessEventsForQueue((MockNotificationQueue) cur);
                    }
                }
                return result;
            } catch (ConcurrentModificationException e) {
                retry--;
            }
        } while (retry > 0);
        return 0;
    }

    private int doProcessEventsForQueue(final MockNotificationQueue queue) {


        int result = 0;
        final List<Notification> processedNotifications = new ArrayList<Notification>();
        final List<Notification> oldNotifications = new ArrayList<Notification>();

        List<Notification> readyNotifications = queue.getReadyNotifications();
        for (final Notification cur : readyNotifications) {
            final NotificationKey key = deserializeEvent(cur.getNotificationKeyClass(), cur.getNotificationKey());
            queue.getHandler().handleReadyNotification(key, cur.getEffectiveDate(), cur.getFutureUserToken(), cur.getAccountRecordId(), cur.getTenantRecordId());
            final DefaultNotification processedNotification = new DefaultNotification(-1L, cur.getId(), getHostname(), getHostname(),
                                                                                      "MockQueue", getClock().getUTCNow().plus(CLAIM_TIME_MS),
                                                                                      PersistentQueueEntryLifecycleState.PROCESSED, cur.getNotificationKeyClass(),
                                                                                      cur.getNotificationKey(), cur.getUserToken(), cur.getFutureUserToken(), cur.getEffectiveDate(),
                                                                                      cur.getAccountRecordId(), cur.getTenantRecordId());
            oldNotifications.add(cur);
            processedNotifications.add(processedNotification);
            result++;
        }

        queue.markProcessedNotifications(oldNotifications, processedNotifications);
        return result;
    }
}
