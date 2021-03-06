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

package com.ning.billing.invoice.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.billing.catalog.api.Currency;
import com.ning.billing.invoice.InvoiceTestSuiteNoDB;
import com.ning.billing.invoice.api.InvoiceItem;
import com.ning.billing.invoice.api.InvoiceItemType;

public class TestItemAdjInvoiceItem extends InvoiceTestSuiteNoDB {

    @Test(groups = "fast")
    public void testType() throws Exception {
        final InvoiceItem invoiceItem = new ItemAdjInvoiceItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                                                               new LocalDate(2010, 1, 1), new BigDecimal("7.00"), Currency.USD,
                                                               UUID.randomUUID());
        Assert.assertEquals(invoiceItem.getInvoiceItemType(), InvoiceItemType.ITEM_ADJ);
    }
}
