/*
 * Copyright 2010-2013 Ning, Inc.
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

package com.ning.billing.osgi.bundles.analytics.dao;

import org.skife.jdbi.v2.DBI;

import com.ning.killbill.osgi.libs.killbill.OSGIKillbillDataSource;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillLogService;

public class BusinessAnalyticsDaoBase {

    protected final OSGIKillbillLogService logService;
    protected final BusinessAnalyticsSqlDao sqlDao;

    public BusinessAnalyticsDaoBase(final OSGIKillbillLogService logService, final OSGIKillbillDataSource osgiKillbillDataSource) {
        final DBI dbi = BusinessDBIProvider.get(osgiKillbillDataSource.getDataSource());
        sqlDao = dbi.onDemand(BusinessAnalyticsSqlDao.class);
        this.logService = logService;
    }
}
