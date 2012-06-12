/*
 * Copyright 2010-2012 Ning, Inc.
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

package com.ning.billing.jaxrs.json;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableList;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.invoice.api.Invoice;
import com.ning.billing.invoice.api.InvoiceItem;

public class TestInvoiceJsonWithItems {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JodaModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test(groups = "fast")
    public void testJson() throws Exception {
        final BigDecimal amount = BigDecimal.TEN;
        final BigDecimal credit = BigDecimal.ONE;
        final String invoiceId = UUID.randomUUID().toString();
        final DateTime invoiceDate = new DateTime(DateTimeZone.UTC);
        final DateTime targetDate = new DateTime(DateTimeZone.UTC);
        final String invoiceNumber = UUID.randomUUID().toString();
        final BigDecimal balance = BigDecimal.ZERO;
        final String accountId = UUID.randomUUID().toString();
        final InvoiceItemJsonSimple invoiceItemJsonSimple = createInvoiceItemJson();
        final InvoiceJsonWithItems invoiceJsonWithItems = new InvoiceJsonWithItems(amount, credit, invoiceId, invoiceDate,
                                                                                   targetDate, invoiceNumber, balance, accountId,
                                                                                   ImmutableList.<InvoiceItemJsonSimple>of(invoiceItemJsonSimple));
        Assert.assertEquals(invoiceJsonWithItems.getAmount(), amount);
        Assert.assertEquals(invoiceJsonWithItems.getCredit(), credit);
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceId(), invoiceId);
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceDate(), invoiceDate);
        Assert.assertEquals(invoiceJsonWithItems.getTargetDate(), targetDate);
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceNumber(), invoiceNumber);
        Assert.assertEquals(invoiceJsonWithItems.getBalance(), balance);
        Assert.assertEquals(invoiceJsonWithItems.getAccountId(), accountId);
        Assert.assertEquals(invoiceJsonWithItems.getItems().size(), 1);
        Assert.assertEquals(invoiceJsonWithItems.getItems().get(0), invoiceItemJsonSimple);

        final String asJson = mapper.writeValueAsString(invoiceJsonWithItems);
        Assert.assertEquals(asJson, "{\"amount\":" + invoiceJsonWithItems.getAmount().toString() + "," +
                "\"credit\":" + invoiceJsonWithItems.getCredit().toString() + "," +
                "\"invoiceId\":\"" + invoiceJsonWithItems.getInvoiceId() + "\"," +
                "\"invoiceDate\":\"" + invoiceJsonWithItems.getInvoiceDate().toDateTimeISO().toString() + "\"," +
                "\"targetDate\":\"" + invoiceJsonWithItems.getTargetDate().toDateTimeISO().toString() + "\"," +
                "\"invoiceNumber\":\"" + invoiceJsonWithItems.getInvoiceNumber() + "\"," +
                "\"balance\":" + invoiceJsonWithItems.getBalance().toString() + "," +
                "\"accountId\":\"" + invoiceJsonWithItems.getAccountId() + "\"," +
                "\"items\":[{\"invoiceId\":\"" + invoiceItemJsonSimple.getInvoiceId().toString() + "\"," +
                "\"accountId\":\"" + invoiceItemJsonSimple.getAccountId().toString() + "\"," +
                "\"bundleId\":\"" + invoiceItemJsonSimple.getBundleId().toString() + "\"," +
                "\"subscriptionId\":\"" + invoiceItemJsonSimple.getSubscriptionId().toString() + "\"," +
                "\"planName\":\"" + invoiceItemJsonSimple.getPlanName() + "\"," +
                "\"phaseName\":\"" + invoiceItemJsonSimple.getPhaseName() + "\"," +
                "\"description\":\"" + invoiceItemJsonSimple.getDescription() + "\"," +
                "\"startDate\":\"" + invoiceItemJsonSimple.getStartDate().toDateTimeISO().toString() + "\"," +
                "\"endDate\":\"" + invoiceItemJsonSimple.getEndDate().toDateTimeISO().toString() + "\"," +
                "\"amount\":" + invoiceItemJsonSimple.getAmount().toString() + "," +
                "\"currency\":\"" + invoiceItemJsonSimple.getCurrency().toString() + "\"}]}");

        final InvoiceJsonWithItems fromJson = mapper.readValue(asJson, InvoiceJsonWithItems.class);
        Assert.assertEquals(fromJson, invoiceJsonWithItems);
    }

    @Test(groups = "fast")
    public void testFromInvoice() throws Exception {
        final Invoice invoice = Mockito.mock(Invoice.class);
        Mockito.when(invoice.getAmountCharged()).thenReturn(BigDecimal.TEN);
        Mockito.when(invoice.getAmountCredited()).thenReturn(BigDecimal.ONE);
        Mockito.when(invoice.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoice.getInvoiceDate()).thenReturn(new DateTime(DateTimeZone.UTC));
        Mockito.when(invoice.getTargetDate()).thenReturn(new DateTime(DateTimeZone.UTC));
        Mockito.when(invoice.getInvoiceNumber()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(invoice.getBalance()).thenReturn(BigDecimal.ZERO);
        Mockito.when(invoice.getAccountId()).thenReturn(UUID.randomUUID());
        final InvoiceItem invoiceItem = createInvoiceItem();
        Mockito.when(invoice.getInvoiceItems()).thenReturn(ImmutableList.<InvoiceItem>of(invoiceItem));

        final InvoiceJsonWithItems invoiceJsonWithItems = new InvoiceJsonWithItems(invoice);
        Assert.assertEquals(invoiceJsonWithItems.getAmount(), invoice.getAmountCharged());
        Assert.assertEquals(invoiceJsonWithItems.getCredit(), invoice.getAmountCredited());
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceId(), invoice.getId().toString());
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceDate(), invoice.getInvoiceDate());
        Assert.assertEquals(invoiceJsonWithItems.getTargetDate(), invoice.getTargetDate());
        Assert.assertEquals(invoiceJsonWithItems.getInvoiceNumber(), String.valueOf(invoice.getInvoiceNumber()));
        Assert.assertEquals(invoiceJsonWithItems.getBalance(), invoice.getBalance());
        Assert.assertEquals(invoiceJsonWithItems.getAccountId(), invoice.getAccountId().toString());
        Assert.assertEquals(invoiceJsonWithItems.getItems().size(), 1);
        final InvoiceItemJsonSimple invoiceItemJsonSimple = invoiceJsonWithItems.getItems().get(0);
        Assert.assertEquals(invoiceItemJsonSimple.getInvoiceId(), invoiceItem.getInvoiceId());
        Assert.assertEquals(invoiceItemJsonSimple.getAccountId(), invoiceItem.getAccountId());
        Assert.assertEquals(invoiceItemJsonSimple.getBundleId(), invoiceItem.getBundleId());
        Assert.assertEquals(invoiceItemJsonSimple.getSubscriptionId(), invoiceItem.getSubscriptionId());
        Assert.assertEquals(invoiceItemJsonSimple.getPlanName(), invoiceItem.getPlanName());
        Assert.assertEquals(invoiceItemJsonSimple.getPhaseName(), invoiceItem.getPhaseName());
        Assert.assertEquals(invoiceItemJsonSimple.getDescription(), invoiceItem.getDescription());
        Assert.assertEquals(invoiceItemJsonSimple.getStartDate(), invoiceItem.getStartDate());
        Assert.assertEquals(invoiceItemJsonSimple.getEndDate(), invoiceItem.getEndDate());
        Assert.assertEquals(invoiceItemJsonSimple.getAmount(), invoiceItem.getAmount());
        Assert.assertEquals(invoiceItemJsonSimple.getCurrency(), invoiceItem.getCurrency());
    }

    private InvoiceItemJsonSimple createInvoiceItemJson() {
        final UUID invoiceId = UUID.randomUUID();
        final UUID accountId = UUID.randomUUID();
        final UUID bundleId = UUID.randomUUID();
        final UUID subscriptionId = UUID.randomUUID();
        final String planName = UUID.randomUUID().toString();
        final String phaseName = UUID.randomUUID().toString();
        final String description = UUID.randomUUID().toString();
        final DateTime startDate = new DateTime(DateTimeZone.UTC);
        final DateTime endDate = new DateTime(DateTimeZone.UTC);
        final BigDecimal amount = BigDecimal.TEN;
        final Currency currency = Currency.MXN;
        return new InvoiceItemJsonSimple(invoiceId, accountId, bundleId, subscriptionId,
                                         planName, phaseName, description, startDate, endDate,
                                         amount, currency);
    }

    private InvoiceItem createInvoiceItem() {
        final InvoiceItem invoiceItem = Mockito.mock(InvoiceItem.class);
        Mockito.when(invoiceItem.getInvoiceId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getAccountId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getBundleId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getSubscriptionId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getPlanName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getPhaseName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getDescription()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getStartDate()).thenReturn(new DateTime(DateTimeZone.UTC));
        Mockito.when(invoiceItem.getEndDate()).thenReturn(new DateTime(DateTimeZone.UTC));
        Mockito.when(invoiceItem.getAmount()).thenReturn(BigDecimal.TEN);
        Mockito.when(invoiceItem.getCurrency()).thenReturn(Currency.EUR);

        return invoiceItem;
    }
}