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

package com.ning.billing.catalog.rules;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ning.billing.catalog.CatalogTestSuiteNoDB;
import com.ning.billing.catalog.DefaultPriceList;
import com.ning.billing.catalog.DefaultProduct;
import com.ning.billing.catalog.MockCatalog;
import com.ning.billing.catalog.api.ActionPolicy;
import com.ning.billing.catalog.api.BillingPeriod;
import com.ning.billing.catalog.api.CatalogApiException;
import com.ning.billing.catalog.api.IllegalPlanChange;
import com.ning.billing.catalog.api.PhaseType;
import com.ning.billing.catalog.api.PlanAlignmentChange;
import com.ning.billing.catalog.api.PlanChangeResult;
import com.ning.billing.catalog.api.PlanPhaseSpecifier;
import com.ning.billing.catalog.api.PlanSpecifier;
import com.ning.billing.catalog.api.PriceListSet;

public class TestPlanRules extends CatalogTestSuiteNoDB {

    private MockCatalog cat = null;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        cat = new MockCatalog();

        final DefaultPriceList priceList2 = cat.getPriceLists().getChildPriceLists()[0];

        final CaseChangePlanPolicy casePolicy = new CaseChangePlanPolicy().setPolicy(ActionPolicy.END_OF_TERM);
        final CaseChangePlanAlignment caseAlignment = new CaseChangePlanAlignment().setAlignment(PlanAlignmentChange.START_OF_SUBSCRIPTION);
        final CasePriceList casePriceList = new CasePriceList().setToPriceList(priceList2);

        cat.getPlanRules().
                setChangeCase(new CaseChangePlanPolicy[]{casePolicy}).
                   setChangeAlignmentCase(new CaseChangePlanAlignment[]{caseAlignment}).
                   setPriceListCase(new CasePriceList[]{casePriceList});
    }

    @Test(groups = "fast")
    public void testCannotChangeToSamePlan() throws CatalogApiException {
        final DefaultProduct product1 = cat.getCurrentProducts()[0];
        final DefaultPriceList priceList1 = cat.findCurrentPriceList(PriceListSet.DEFAULT_PRICELIST_NAME);

        final PlanPhaseSpecifier from = new PlanPhaseSpecifier(product1.getName(), product1.getCategory(), BillingPeriod.MONTHLY, priceList1.getName(), PhaseType.EVERGREEN);
        final PlanSpecifier to = new PlanSpecifier(product1.getName(), product1.getCategory(), BillingPeriod.MONTHLY, priceList1.getName());

        try {
            cat.getPlanRules().planChange(from, to, cat);
            Assert.fail("We did not see an exception when  trying to change plan to the same plan");
        } catch (IllegalPlanChange e) {
            // Correct - cannot change to the same plan
        } catch (CatalogApiException e) {
            Assert.fail("", e);
        }
    }

    @Test(groups = "fast")
    public void testExistingPriceListIsKept() throws CatalogApiException {
        final DefaultProduct product1 = cat.getCurrentProducts()[0];
        final DefaultPriceList priceList1 = cat.findCurrentPriceList(PriceListSet.DEFAULT_PRICELIST_NAME);

        final PlanPhaseSpecifier from = new PlanPhaseSpecifier(product1.getName(), product1.getCategory(), BillingPeriod.MONTHLY, priceList1.getName(), PhaseType.EVERGREEN);
        final PlanSpecifier to = new PlanSpecifier(product1.getName(), product1.getCategory(), BillingPeriod.ANNUAL, priceList1.getName());

        PlanChangeResult result = null;
        try {
            result = cat.getPlanRules().planChange(from, to, cat);
        } catch (IllegalPlanChange e) {
            Assert.fail("We should not have triggered this error");
        } catch (CatalogApiException e) {
            Assert.fail("", e);
        }

        Assert.assertEquals(result.getPolicy(), ActionPolicy.END_OF_TERM);
        Assert.assertEquals(result.getAlignment(), PlanAlignmentChange.START_OF_SUBSCRIPTION);
        Assert.assertEquals(result.getNewPriceList(), priceList1);
    }

    @Test(groups = "fast")
    public void testBaseCase() throws CatalogApiException {
        final DefaultProduct product1 = cat.getCurrentProducts()[0];
        final DefaultProduct product2 = cat.getCurrentProducts()[1];
        final DefaultPriceList priceList1 = cat.findCurrentPriceList(PriceListSet.DEFAULT_PRICELIST_NAME);
        final DefaultPriceList priceList2 = cat.getPriceLists().getChildPriceLists()[0];

        final PlanPhaseSpecifier from = new PlanPhaseSpecifier(product1.getName(), product1.getCategory(), BillingPeriod.MONTHLY, priceList1.getName(), PhaseType.EVERGREEN);
        final PlanSpecifier to = new PlanSpecifier(product2.getName(), product2.getCategory(), BillingPeriod.MONTHLY, null);

        PlanChangeResult result = null;
        try {
            result = cat.getPlanRules().planChange(from, to, cat);
        } catch (IllegalPlanChange e) {
            Assert.fail("We should not have triggered this error");
        } catch (CatalogApiException e) {
            Assert.fail("", e);
        }

        Assert.assertEquals(result.getPolicy(), ActionPolicy.END_OF_TERM);
        Assert.assertEquals(result.getAlignment(), PlanAlignmentChange.START_OF_SUBSCRIPTION);
        Assert.assertEquals(result.getNewPriceList(), priceList2);
    }
}
