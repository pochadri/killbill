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

package com.ning.billing.beatrix.glue;

import com.ning.billing.beatrix.DefaultBeatrixService;
import com.ning.billing.beatrix.bus.api.BeatrixService;
import com.ning.billing.beatrix.bus.api.ExternalBus;
import com.ning.billing.beatrix.extbus.BeatrixListener;
import com.ning.billing.beatrix.extbus.PersistentExternalBus;
import com.ning.billing.beatrix.lifecycle.DefaultLifecycle;
import com.ning.billing.beatrix.lifecycle.Lifecycle;

import com.google.inject.AbstractModule;

public class BeatrixModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
        installExternalBus();
    }

    protected void installExternalBus() {
        bind(BeatrixService.class).to(DefaultBeatrixService.class);
        bind(DefaultBeatrixService.class).asEagerSingleton();
        bind(ExternalBus.class).to(PersistentExternalBus.class).asEagerSingleton();
        bind(BeatrixListener.class).asEagerSingleton();
    }
}
