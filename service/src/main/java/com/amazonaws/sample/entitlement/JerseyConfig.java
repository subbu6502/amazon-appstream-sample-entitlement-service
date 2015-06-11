/*
 * Copyright 2012-2015 the original author or authors.
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

package com.amazonaws.sample.entitlement;

import com.amazonaws.sample.entitlement.rs.JaxRsAdministrationService;
import com.amazonaws.sample.entitlement.rs.JaxRsEntitlementService;
import com.amazonaws.sample.entitlement.rs.JaxRsIdentityService;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {



	public JerseyConfig() {
        register(JaxRsEntitlementService.class);
		register(JaxRsAdministrationService.class);
		register(JaxRsIdentityService.class);
	}
}

