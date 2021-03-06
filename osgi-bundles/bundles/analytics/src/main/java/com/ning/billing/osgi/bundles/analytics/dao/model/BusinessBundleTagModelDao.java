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

package com.ning.billing.osgi.bundles.analytics.dao.model;

import java.util.UUID;

import javax.annotation.Nullable;

import com.ning.billing.account.api.Account;
import com.ning.billing.util.audit.AuditLog;
import com.ning.billing.util.tag.Tag;
import com.ning.billing.util.tag.TagDefinition;

public class BusinessBundleTagModelDao extends BusinessTagModelDao {

    private UUID bundleId;

    public BusinessBundleTagModelDao() { /* When reading from the database */ }

    public BusinessBundleTagModelDao(final Account account,
                                     final Long accountRecordId,
                                     final Tag tag,
                                     final Long tagRecordId,
                                     final TagDefinition tagDefinition,
                                     @Nullable final AuditLog creationAuditLog,
                                     final Long tenantRecordId,
                                     @Nullable final ReportGroup reportGroup) {
        super(account,
              accountRecordId,
              tag,
              tagRecordId,
              tagDefinition,
              creationAuditLog,
              tenantRecordId,
              reportGroup);
        this.bundleId = tag.getObjectId();
    }

    @Override
    public String getTableName() {
        return BUNDLE_TAGS_TABLE_NAME;
    }

    public UUID getBundleId() {
        return bundleId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BusinessBundleTagModelDao");
        sb.append("{bundleId=").append(bundleId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final BusinessBundleTagModelDao that = (BusinessBundleTagModelDao) o;

        if (bundleId != null ? !bundleId.equals(that.bundleId) : that.bundleId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (bundleId != null ? bundleId.hashCode() : 0);
        return result;
    }
}
