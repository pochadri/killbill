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

package com.ning.billing.util.tag.dao;

import java.util.List;
import java.util.UUID;

import com.ning.billing.ObjectType;
import com.ning.billing.util.api.TagApiException;
import com.ning.billing.util.callcontext.InternalCallContext;
import com.ning.billing.util.callcontext.InternalTenantContext;

public interface TagDao {

    void create(TagModelDao tag, InternalCallContext context) throws TagApiException;

    void deleteTag(UUID objectId, ObjectType objectType, UUID tagDefinition, InternalCallContext context) throws TagApiException;

    TagModelDao getById(UUID tagId, InternalTenantContext context);

    List<TagModelDao> getTagsForObject(UUID objectId, ObjectType objectType, InternalTenantContext internalTenantContext);

    List<TagModelDao> getTagsForAccountType(UUID accountId, ObjectType objectType, InternalTenantContext internalTenantContext);

    List<TagModelDao> getTagsForAccount(UUID accountId, InternalTenantContext internalTenantContext);
}
