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

package com.ning.billing.invoice.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.billing.ErrorCode;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.invoice.api.Invoice;
import com.ning.billing.invoice.api.InvoiceApiException;
import com.ning.billing.invoice.api.InvoiceItemType;
import com.ning.billing.invoice.api.InvoicePaymentType;
import com.ning.billing.invoice.api.user.DefaultInvoiceAdjustmentEvent;
import com.ning.billing.invoice.notification.NextBillingDatePoster;
import com.ning.billing.util.cache.CacheControllerDispatcher;
import com.ning.billing.util.callcontext.InternalCallContext;
import com.ning.billing.util.callcontext.InternalTenantContext;
import com.ning.billing.util.clock.Clock;
import com.ning.billing.util.dao.NonEntityDao;
import com.ning.billing.util.entity.dao.EntityDaoBase;
import com.ning.billing.util.entity.dao.EntitySqlDao;
import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionWrapper;
import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionalJdbiWrapper;
import com.ning.billing.util.entity.dao.EntitySqlDaoWrapperFactory;
import com.ning.billing.util.svcsapi.bus.InternalBus;
import com.ning.billing.util.svcsapi.bus.InternalBus.EventBusException;

import com.google.inject.Inject;

public class DefaultInvoiceDao extends EntityDaoBase<InvoiceModelDao, Invoice, InvoiceApiException> implements InvoiceDao {

    private static final Logger log = LoggerFactory.getLogger(DefaultInvoiceDao.class);

    private final NextBillingDatePoster nextBillingDatePoster;
    private final InternalBus eventBus;
    private final InvoiceDaoHelper invoiceDaoHelper;
    private final CBADao cbaDao;

    @Inject
    public DefaultInvoiceDao(final IDBI dbi,
                             final NextBillingDatePoster nextBillingDatePoster,
                             final InternalBus eventBus,
                             final Clock clock,
                             final CacheControllerDispatcher cacheControllerDispatcher,
                             final NonEntityDao nonEntityDao) {
        super(new EntitySqlDaoTransactionalJdbiWrapper(dbi, clock, cacheControllerDispatcher, nonEntityDao), InvoiceSqlDao.class);
        this.nextBillingDatePoster = nextBillingDatePoster;
        this.eventBus = eventBus;
        this.invoiceDaoHelper = new InvoiceDaoHelper();
        this.cbaDao =  new CBADao();
    }

    @Override
    protected InvoiceApiException generateAlreadyExistsException(final InvoiceModelDao entity, final InternalCallContext context) {
        return new InvoiceApiException(ErrorCode.INVOICE_ACCOUNT_ID_INVALID, entity.getId());
    }

    @Override
    public List<InvoiceModelDao> getInvoicesByAccount(final UUID accountId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final List<InvoiceModelDao> invoices = invoiceDao.getInvoicesByAccount(accountId.toString(), context);
                invoiceDaoHelper.populateChildren(invoices, entitySqlDaoWrapperFactory, context);

                return invoices;
            }
        });
    }

    @Override
    public List<InvoiceModelDao> getAllInvoicesByAccount(final UUID accountId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return invoiceDaoHelper.getAllInvoicesByAccountFromTransaction(accountId, entitySqlDaoWrapperFactory, context);
            }
        });
    }

    @Override
    public List<InvoiceModelDao> getInvoicesByAccount(final UUID accountId, final LocalDate fromDate, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final List<InvoiceModelDao> invoices = invoiceDao.getInvoicesByAccountAfterDate(accountId.toString(),
                                                                                                fromDate.toDateTimeAtStartOfDay().toDate(),
                                                                                                context);
                invoiceDaoHelper.populateChildren(invoices, entitySqlDaoWrapperFactory, context);

                return invoices;
            }
        });
    }

    @Override
    public List<InvoiceModelDao> get(final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final List<InvoiceModelDao> invoices = invoiceDao.get(context);
                invoiceDaoHelper.populateChildren(invoices, entitySqlDaoWrapperFactory, context);

                return invoices;
            }
        });
    }

    @Override
    public InvoiceModelDao getById(final UUID invoiceId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceModelDao>() {
            @Override
            public InvoiceModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final InvoiceModelDao invoice = invoiceDao.getById(invoiceId.toString(), context);
                if (invoice == null) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_NOT_FOUND, invoiceId);
                }
                invoiceDaoHelper.populateChildren(invoice, entitySqlDaoWrapperFactory, context);
                return invoice;
            }
        });
    }

    @Override
    public InvoiceModelDao getByNumber(final Integer number, final InternalTenantContext context) throws InvoiceApiException {

        if (number == null) {
            throw new InvoiceApiException(ErrorCode.INVOICE_INVALID_NUMBER, "(null)");
        }

        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceModelDao>() {
            @Override
            public InvoiceModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final InvoiceModelDao invoice = invoiceDao.getByRecordId(number.longValue(), context);
                if (invoice == null) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_NUMBER_NOT_FOUND, number.longValue());
                }
                invoiceDaoHelper.populateChildren(invoice, entitySqlDaoWrapperFactory, context);
                return invoice;
            }
        });
    }

    @Override
    public void createInvoice(final InvoiceModelDao invoice, final List<InvoiceItemModelDao> invoiceItems,
                              final List<InvoicePaymentModelDao> invoicePayments, final boolean isRealInvoice, final Map<UUID, DateTime> callbackDateTimePerSubscriptions,
                              final InternalCallContext context) {
        transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final InvoiceModelDao currentInvoice = transactional.getById(invoice.getId().toString(), context);
                if (currentInvoice == null) {
                    // We only want to insert that invoice if there are real invoiceItems associated to it -- if not, this is just
                    // a shell invoice and we only need to insert the invoiceItems -- for the already existing invoices
                    if (isRealInvoice) {
                        transactional.create(invoice, context);
                    }

                    // Create the invoice items
                    final InvoiceItemSqlDao transInvoiceItemSqlDao = entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class);
                    for (final InvoiceItemModelDao invoiceItemModelDao : invoiceItems) {
                        transInvoiceItemSqlDao.create(invoiceItemModelDao, context);
                    }

                    cbaDao.doCBAComplexity(invoice.getAccountId(), entitySqlDaoWrapperFactory, context);

                    notifyOfFutureBillingEvents(entitySqlDaoWrapperFactory, invoice.getAccountId(), callbackDateTimePerSubscriptions, context.getUserToken());

                    // Create associated payments
                    final InvoicePaymentSqlDao invoicePaymentSqlDao = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class);
                    invoicePaymentSqlDao.batchCreateFromTransaction(invoicePayments, context);

                }
                return null;
            }
        });
    }

    @Override
    public List<InvoiceModelDao> getInvoicesBySubscription(final UUID subscriptionId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao invoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final List<InvoiceModelDao> invoices = invoiceDao.getInvoicesBySubscription(subscriptionId.toString(), context);
                invoiceDaoHelper.populateChildren(invoices, entitySqlDaoWrapperFactory, context);

                return invoices;
            }
        });
    }

    @Override
    public BigDecimal getAccountBalance(final UUID accountId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<BigDecimal>() {
            @Override
            public BigDecimal inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                BigDecimal cba = BigDecimal.ZERO;

                BigDecimal accountBalance = BigDecimal.ZERO;
                final List<InvoiceModelDao> invoices = invoiceDaoHelper.getAllInvoicesByAccountFromTransaction(accountId, entitySqlDaoWrapperFactory, context);
                for (final InvoiceModelDao cur : invoices) {
                    accountBalance = accountBalance.add(InvoiceModelDaoHelper.getBalance(cur));
                    cba = cba.add(InvoiceModelDaoHelper.getCBAAmount(cur));
                }
                return accountBalance.subtract(cba);
            }
        });
    }

    @Override
    public BigDecimal getAccountCBA(final UUID accountId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<BigDecimal>() {
            @Override
            public BigDecimal inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return cbaDao.getAccountCBAFromTransaction(accountId, entitySqlDaoWrapperFactory, context);
            }
        });
    }

    @Override
    public List<InvoiceModelDao> getUnpaidInvoicesByAccountId(final UUID accountId, @Nullable final LocalDate upToDate, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoiceModelDao>>() {
            @Override
            public List<InvoiceModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return invoiceDaoHelper.getUnpaidInvoicesByAccountFromTransaction(accountId, entitySqlDaoWrapperFactory, upToDate, context);
            }
        });
    }


    @Override
    public UUID getInvoiceIdByPaymentId(final UUID paymentId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<UUID>() {
            @Override
            public UUID inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class).getInvoiceIdByPaymentId(paymentId.toString(), context);
            }
        });
    }

    @Override
    public List<InvoicePaymentModelDao> getInvoicePayments(final UUID paymentId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoicePaymentModelDao>>() {
            @Override
            public List<InvoicePaymentModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getInvoicePayments(paymentId.toString(), context);
            }
        });
    }

    @Override
    public InvoicePaymentModelDao createRefund(final UUID paymentId, final BigDecimal requestedRefundAmount, final boolean isInvoiceAdjusted,
                                               final Map<UUID, BigDecimal> invoiceItemIdsWithNullAmounts, final UUID paymentCookieId,
                                               final InternalCallContext context)
            throws InvoiceApiException {
        final boolean isInvoiceItemAdjusted = isInvoiceAdjusted && invoiceItemIdsWithNullAmounts.size() > 0;

        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoicePaymentModelDao>() {
            @Override
            public InvoicePaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoicePaymentSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class);

                final InvoiceSqlDao transInvoiceDao = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                final InvoicePaymentModelDao payment = transactional.getByPaymentId(paymentId.toString(), context);
                if (payment == null) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_PAYMENT_BY_ATTEMPT_NOT_FOUND, paymentId);
                }

                // Retrieve the amounts to adjust, if needed
                final Map<UUID, BigDecimal> invoiceItemIdsWithAmounts = invoiceDaoHelper.computeItemAdjustments(payment.getInvoiceId().toString(),
                                                                                               entitySqlDaoWrapperFactory,
                                                                                               invoiceItemIdsWithNullAmounts,
                                                                                               context);

                // Compute the actual amount to refund
                final BigDecimal requestedPositiveAmount = invoiceDaoHelper.computePositiveRefundAmount(payment, requestedRefundAmount, invoiceItemIdsWithAmounts);

                // Before we go further, check if that refund already got inserted -- the payment system keeps a state machine
                // and so this call may be called several time for the same  paymentCookieId (which is really the refundId)
                final InvoicePaymentModelDao existingRefund = transactional.getPaymentsForCookieId(paymentCookieId.toString(), context);
                if (existingRefund != null) {
                    return existingRefund;
                }

                final InvoicePaymentModelDao refund = new InvoicePaymentModelDao(UUID.randomUUID(), context.getCreatedDate(), InvoicePaymentType.REFUND,
                                                                                 payment.getInvoiceId(), paymentId,
                                                                                 context.getCreatedDate(), requestedPositiveAmount.negate(),
                                                                                 payment.getCurrency(), paymentCookieId, payment.getId());
                transactional.create(refund, context);

                // Retrieve invoice after the Refund
                final InvoiceModelDao invoice = transInvoiceDao.getById(payment.getInvoiceId().toString(), context);
                if (invoice != null) {
                    invoiceDaoHelper.populateChildren(invoice, entitySqlDaoWrapperFactory, context);
                } else {
                    throw new IllegalStateException("Invoice shouldn't be null for payment " + payment.getId());
                }

                final BigDecimal invoiceBalanceAfterRefund = InvoiceModelDaoHelper.getBalance(invoice);
                final InvoiceItemSqlDao transInvoiceItemDao = entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class);

                // At this point, we created the refund which made the invoice balance positive and applied any existing
                // available CBA to that invoice.
                // We now need to adjust the invoice and/or invoice items if needed and specified.
                if (isInvoiceAdjusted && !isInvoiceItemAdjusted) {
                    // Invoice adjustment
                    final BigDecimal maxBalanceToAdjust = (invoiceBalanceAfterRefund.compareTo(BigDecimal.ZERO) <= 0) ? BigDecimal.ZERO : invoiceBalanceAfterRefund;
                    final BigDecimal requestedPositiveAmountToAdjust = requestedPositiveAmount.compareTo(maxBalanceToAdjust) > 0 ? maxBalanceToAdjust : requestedPositiveAmount;
                    if (requestedPositiveAmountToAdjust.compareTo(BigDecimal.ZERO) > 0) {
                        final InvoiceItemModelDao adjItem = new InvoiceItemModelDao(context.getCreatedDate(), InvoiceItemType.REFUND_ADJ, invoice.getId(), invoice.getAccountId(),
                                                                                    null, null, null, null, context.getCreatedDate().toLocalDate(), null,
                                                                                    requestedPositiveAmountToAdjust.negate(), null, invoice.getCurrency(), null);
                        transInvoiceItemDao.create(adjItem, context);
                    }
                } else if (isInvoiceAdjusted) {
                    // Invoice item adjustment
                    for (final UUID invoiceItemId : invoiceItemIdsWithAmounts.keySet()) {
                        final BigDecimal adjAmount = invoiceItemIdsWithAmounts.get(invoiceItemId);
                        final InvoiceItemModelDao item = invoiceDaoHelper.createAdjustmentItem(entitySqlDaoWrapperFactory, invoice.getId(), invoiceItemId, adjAmount,
                                                                              invoice.getCurrency(), context.getCreatedDate().toLocalDate(),
                                                                              context);
                        transInvoiceItemDao.create(item, context);
                    }
                }

                cbaDao.doCBAComplexity(invoice.getAccountId(), entitySqlDaoWrapperFactory, context);

                // Notify the bus since the balance of the invoice changed
                notifyBusOfInvoiceAdjustment(entitySqlDaoWrapperFactory, invoice.getId(), invoice.getAccountId(), context.getUserToken(), context);

                return refund;
            }
        });
    }



    @Override
    public InvoicePaymentModelDao postChargeback(final UUID invoicePaymentId, final BigDecimal amount, final InternalCallContext context) throws InvoiceApiException {

        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoicePaymentModelDao>() {
            @Override
            public InvoicePaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoicePaymentSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class);

                final BigDecimal maxChargedBackAmount = invoiceDaoHelper.getRemainingAmountPaidFromTransaction(invoicePaymentId, entitySqlDaoWrapperFactory, context);
                final BigDecimal requestedChargedBackAmout = (amount == null) ? maxChargedBackAmount : amount;
                if (requestedChargedBackAmout.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvoiceApiException(ErrorCode.CHARGE_BACK_AMOUNT_IS_NEGATIVE);
                }
                if (requestedChargedBackAmout.compareTo(maxChargedBackAmount) > 0) {
                    throw new InvoiceApiException(ErrorCode.CHARGE_BACK_AMOUNT_TOO_HIGH, requestedChargedBackAmout, maxChargedBackAmount);
                }

                final InvoicePaymentModelDao payment = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getById(invoicePaymentId.toString(), context);
                if (payment == null) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_PAYMENT_NOT_FOUND, invoicePaymentId.toString());
                }
                final InvoicePaymentModelDao chargeBack = new InvoicePaymentModelDao(UUID.randomUUID(), context.getCreatedDate(), InvoicePaymentType.CHARGED_BACK,
                                                                                     payment.getInvoiceId(), payment.getPaymentId(), context.getCreatedDate(),
                                                                                     requestedChargedBackAmout.negate(), payment.getCurrency(), null, payment.getId());
                transactional.create(chargeBack, context);

                // Notify the bus since the balance of the invoice changed
                final UUID accountId = transactional.getAccountIdFromInvoicePaymentId(chargeBack.getId().toString(), context);

                cbaDao.doCBAComplexity(accountId, entitySqlDaoWrapperFactory, context);

                notifyBusOfInvoiceAdjustment(entitySqlDaoWrapperFactory, payment.getInvoiceId(), accountId, context.getUserToken(), context);

                return chargeBack;
            }
        });
    }

    @Override
    public BigDecimal getRemainingAmountPaid(final UUID invoicePaymentId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<BigDecimal>() {
            @Override
            public BigDecimal inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return invoiceDaoHelper.getRemainingAmountPaidFromTransaction(invoicePaymentId, entitySqlDaoWrapperFactory, context);
            }
        });
    }

    @Override
    public UUID getAccountIdFromInvoicePaymentId(final UUID invoicePaymentId, final InternalTenantContext context) throws InvoiceApiException {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<UUID>() {
            @Override
            public UUID inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final UUID accountId = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getAccountIdFromInvoicePaymentId(invoicePaymentId.toString(), context);
                if (accountId == null) {
                    throw new InvoiceApiException(ErrorCode.CHARGE_BACK_COULD_NOT_FIND_ACCOUNT_ID, invoicePaymentId);
                } else {
                    return accountId;
                }
            }
        });
    }

    @Override
    public List<InvoicePaymentModelDao> getChargebacksByAccountId(final UUID accountId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoicePaymentModelDao>>() {
            @Override
            public List<InvoicePaymentModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getChargeBacksByAccountId(accountId.toString(), context);
            }
        });
    }

    @Override
    public List<InvoicePaymentModelDao> getChargebacksByPaymentId(final UUID paymentId, final InternalTenantContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<List<InvoicePaymentModelDao>>() {
            @Override
            public List<InvoicePaymentModelDao> inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getChargebacksByPaymentId(paymentId.toString(), context);
            }
        });
    }

    @Override
    public InvoicePaymentModelDao getChargebackById(final UUID chargebackId, final InternalTenantContext context) throws InvoiceApiException {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoicePaymentModelDao>() {
            @Override
            public InvoicePaymentModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoicePaymentModelDao chargeback = entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).getById(chargebackId.toString(), context);
                if (chargeback == null) {
                    throw new InvoiceApiException(ErrorCode.CHARGE_BACK_DOES_NOT_EXIST, chargebackId);
                } else {
                    return chargeback;
                }
            }
        });
    }

    @Override
    public InvoiceItemModelDao getExternalChargeById(final UUID externalChargeId, final InternalTenantContext context) throws InvoiceApiException {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceItemModelDao>() {
            @Override
            public InvoiceItemModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class).getById(externalChargeId.toString(), context);
            }
        });
    }

    @Override
    public void notifyOfPayment(final InvoicePaymentModelDao invoicePayment, final InternalCallContext context) {
        transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                entitySqlDaoWrapperFactory.become(InvoicePaymentSqlDao.class).create(invoicePayment, context);
                return null;
            }
        });
    }

    @Override
    public InvoiceItemModelDao insertExternalCharge(final UUID accountId, @Nullable final UUID invoiceId, @Nullable final UUID bundleId, final String description,
                                                    final BigDecimal amount, final LocalDate effectiveDate, final Currency currency, final InternalCallContext context)
            throws InvoiceApiException {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceItemModelDao>() {
            @Override
            public InvoiceItemModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                UUID invoiceIdForExternalCharge = invoiceId;
                // Create an invoice for that external charge if it doesn't exist
                if (invoiceIdForExternalCharge == null) {
                    final InvoiceModelDao invoiceForExternalCharge = new InvoiceModelDao(accountId, effectiveDate, effectiveDate, currency);
                    transactional.create(invoiceForExternalCharge, context);
                    invoiceIdForExternalCharge = invoiceForExternalCharge.getId();
                }

                final InvoiceItemModelDao externalCharge = new InvoiceItemModelDao(context.getCreatedDate(), InvoiceItemType.EXTERNAL_CHARGE,
                                                                                   invoiceIdForExternalCharge, accountId,
                                                                                   bundleId, null, description, null,
                                                                                   effectiveDate, null, amount, null,
                                                                                   currency, null);
                final InvoiceItemSqlDao transInvoiceItemDao = entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class);
                transInvoiceItemDao.create(externalCharge, context);


                cbaDao.doCBAComplexity(accountId, entitySqlDaoWrapperFactory, context);

                // Notify the bus since the balance of the invoice changed
                // TODO should we post an InvoiceCreationInternalEvent event instead? Note! This will trigger a payment (see InvoiceHandler)
                notifyBusOfInvoiceAdjustment(entitySqlDaoWrapperFactory, invoiceId, accountId, context.getUserToken(), context);

                return externalCharge;
            }
        });
    }

    @Override
    public InvoiceItemModelDao getCreditById(final UUID creditId, final InternalTenantContext context) throws InvoiceApiException {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceItemModelDao>() {
            @Override
            public InvoiceItemModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                return entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class).getById(creditId.toString(), context);
            }
        });
    }

    @Override
    public InvoiceItemModelDao insertCredit(final UUID accountId, @Nullable final UUID invoiceId, final BigDecimal positiveCreditAmount,
                                            final LocalDate effectiveDate, final Currency currency, final InternalCallContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceItemModelDao>() {
            @Override
            public InvoiceItemModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                UUID invoiceIdForCredit = invoiceId;
                // Create an invoice for that credit if it doesn't exist
                if (invoiceIdForCredit == null) {
                    final InvoiceModelDao invoiceForCredit = new InvoiceModelDao(accountId, effectiveDate, effectiveDate, currency);
                    transactional.create(invoiceForCredit, context);
                    invoiceIdForCredit = invoiceForCredit.getId();
                }

                // Note! The amount is negated here!
                final InvoiceItemModelDao credit = new InvoiceItemModelDao(context.getCreatedDate(), InvoiceItemType.CREDIT_ADJ, invoiceIdForCredit,
                                                                           accountId, null, null, null, null, effectiveDate,
                                                                           null, positiveCreditAmount.negate(), null,
                                                                           currency, null);
                invoiceDaoHelper.insertItem(entitySqlDaoWrapperFactory, credit, context);

                cbaDao.doCBAComplexity(accountId, entitySqlDaoWrapperFactory, context);

                // Notify the bus since the balance of the invoice changed
                notifyBusOfInvoiceAdjustment(entitySqlDaoWrapperFactory, invoiceId, accountId, context.getUserToken(), context);

                return credit;
            }
        });
    }

    @Override
    public InvoiceItemModelDao insertInvoiceItemAdjustment(final UUID accountId, final UUID invoiceId, final UUID invoiceItemId,
                                                           final LocalDate effectiveDate, @Nullable final BigDecimal positiveAdjAmount,
                                                           @Nullable final Currency currency, final InternalCallContext context) {
        return transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<InvoiceItemModelDao>() {
            @Override
            public InvoiceItemModelDao inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceItemModelDao invoiceItemAdjustment = invoiceDaoHelper.createAdjustmentItem(entitySqlDaoWrapperFactory, invoiceId, invoiceItemId, positiveAdjAmount,
                                                                                       currency, effectiveDate, context);
                invoiceDaoHelper.insertItem(entitySqlDaoWrapperFactory, invoiceItemAdjustment, context);

                cbaDao.doCBAComplexity(accountId, entitySqlDaoWrapperFactory, context);

                notifyBusOfInvoiceAdjustment(entitySqlDaoWrapperFactory, invoiceId, accountId, context.getUserToken(), context);

                return invoiceItemAdjustment;
            }
        });
    }

    @Override
    public void deleteCBA(final UUID accountId, final UUID invoiceId, final UUID invoiceItemId, final InternalCallContext context) throws InvoiceApiException {
        transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                final InvoiceSqlDao transactional = entitySqlDaoWrapperFactory.become(InvoiceSqlDao.class);

                // Retrieve the invoice and make sure it belongs to the right account
                final InvoiceModelDao invoice = transactional.getById(invoiceId.toString(), context);
                if (invoice == null || !invoice.getAccountId().equals(accountId)) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_NOT_FOUND, invoiceId);
                }

                // Retrieve the invoice item and make sure it belongs to the right invoice
                final InvoiceItemSqlDao invoiceItemSqlDao = entitySqlDaoWrapperFactory.become(InvoiceItemSqlDao.class);
                final InvoiceItemModelDao cbaItem = invoiceItemSqlDao.getById(invoiceItemId.toString(), context);
                if (cbaItem == null || !cbaItem.getInvoiceId().equals(invoice.getId())) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_ITEM_NOT_FOUND, invoiceItemId);
                }

                // First, adjust the same invoice with the CBA amount to "delete"
                final InvoiceItemModelDao cbaAdjItem = new InvoiceItemModelDao(context.getCreatedDate(), InvoiceItemType.CBA_ADJ, invoice.getId(), invoice.getAccountId(),
                                                                               null, null, null, null, context.getCreatedDate().toLocalDate(),
                                                                               null, cbaItem.getAmount().negate(), null, cbaItem.getCurrency(), cbaItem.getId());
                invoiceItemSqlDao.create(cbaAdjItem, context);

                // Verify the final invoice balance is not negative
                invoiceDaoHelper.populateChildren(invoice, entitySqlDaoWrapperFactory, context);
                if (InvoiceModelDaoHelper.getBalance(invoice).compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvoiceApiException(ErrorCode.INVOICE_WOULD_BE_NEGATIVE);
                }

                // If there is more account credit than CBA we adjusted, we're done.
                // Otherwise, we need to find further invoices on which this credit was consumed
                final BigDecimal accountCBA = cbaDao.getAccountCBAFromTransaction(accountId, entitySqlDaoWrapperFactory, context);
                if (accountCBA.compareTo(BigDecimal.ZERO) < 0) {
                    if (accountCBA.compareTo(cbaItem.getAmount().negate()) < 0) {
                        throw new IllegalStateException("The account balance can't be lower than the amount adjusted");
                    }
                    final List<InvoiceModelDao> invoicesFollowing = transactional.getInvoicesByAccountAfterDate(accountId.toString(),
                                                                                                                invoice.getInvoiceDate().toDateTimeAtStartOfDay().toDate(),
                                                                                                                context);
                    invoiceDaoHelper.populateChildren(invoicesFollowing, entitySqlDaoWrapperFactory, context);

                    // The remaining amount to adjust (i.e. the amount of credits used on following invoices)
                    // is the current account CBA balance (minus the sign)
                    BigDecimal positiveRemainderToAdjust = accountCBA.negate();
                    for (final InvoiceModelDao invoiceFollowing : invoicesFollowing) {
                        if (invoiceFollowing.getId().equals(invoice.getId())) {
                            continue;
                        }

                        // Add a single adjustment per invoice
                        BigDecimal positiveCBAAdjItemAmount = BigDecimal.ZERO;

                        for (final InvoiceItemModelDao cbaUsed : invoiceFollowing.getInvoiceItems()) {
                            // Ignore non CBA items or credits (CBA >= 0)
                            if (!InvoiceItemType.CBA_ADJ.equals(cbaUsed.getType()) ||
                                cbaUsed.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                                continue;
                            }

                            final BigDecimal positiveCBAUsedAmount = cbaUsed.getAmount().negate();
                            final BigDecimal positiveNextCBAAdjItemAmount;
                            if (positiveCBAUsedAmount.compareTo(positiveRemainderToAdjust) < 0) {
                                positiveNextCBAAdjItemAmount = positiveCBAUsedAmount;
                                positiveRemainderToAdjust = positiveRemainderToAdjust.subtract(positiveNextCBAAdjItemAmount);
                            } else {
                                positiveNextCBAAdjItemAmount = positiveRemainderToAdjust;
                                positiveRemainderToAdjust = BigDecimal.ZERO;
                            }
                            positiveCBAAdjItemAmount = positiveCBAAdjItemAmount.add(positiveNextCBAAdjItemAmount);

                            if (positiveRemainderToAdjust.compareTo(BigDecimal.ZERO) == 0) {
                                break;
                            }
                        }

                        // Add the adjustment on that invoice
                        final InvoiceItemModelDao nextCBAAdjItem = new InvoiceItemModelDao(context.getCreatedDate(), InvoiceItemType.CBA_ADJ, invoiceFollowing.getId(),
                                                                                           invoice.getAccountId(), null, null, null, null,
                                                                                           context.getCreatedDate().toLocalDate(), null,
                                                                                           positiveCBAAdjItemAmount, null, cbaItem.getCurrency(), cbaItem.getId());
                        invoiceItemSqlDao.create(nextCBAAdjItem, context);
                        if (positiveRemainderToAdjust.compareTo(BigDecimal.ZERO) == 0) {
                            break;
                        }
                    }
                }

                return null;
            }
        });
    }

    public void consumeExstingCBAOnAccountWithUnpaidInvoices(final UUID accountId, final InternalCallContext context) {
        transactionalSqlDao.execute(new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                // In theory we should only have to call useExistingCBAFromTransaction but just to be safe we also check for credit generation
                cbaDao.doCBAComplexity(accountId, entitySqlDaoWrapperFactory, context);
                return null;
            }
        });
    }

    private void notifyOfFutureBillingEvents(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory, final UUID accountId,
                                             final Map<UUID, DateTime> callbackDateTimePerSubscriptions, final UUID userToken) {
        for (final UUID subscriptionId : callbackDateTimePerSubscriptions.keySet()) {
            final DateTime callbackDateTimeUTC = callbackDateTimePerSubscriptions.get(subscriptionId);
            nextBillingDatePoster.insertNextBillingNotification(entitySqlDaoWrapperFactory, accountId, subscriptionId, callbackDateTimeUTC, userToken);
        }
    }

    private void notifyBusOfInvoiceAdjustment(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory, final UUID invoiceId, final UUID accountId,
                                              final UUID userToken, final InternalCallContext context) {
        try {
            eventBus.postFromTransaction(new DefaultInvoiceAdjustmentEvent(invoiceId, accountId, userToken, context.getAccountRecordId(), context.getTenantRecordId()),
                                         entitySqlDaoWrapperFactory, context);
        } catch (EventBusException e) {
            log.warn("Failed to post adjustment event for invoice " + invoiceId, e);
        }
    }

}
