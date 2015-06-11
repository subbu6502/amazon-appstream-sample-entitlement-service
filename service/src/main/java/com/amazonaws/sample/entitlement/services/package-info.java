/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use
 * this file except in compliance with the License. A copy of the License is
 * located at:
 *
 *       http://aws.amazon.com/asl/
 *
 * This Software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * <p>Provides classes to do the primary work of the entitlement service.</p>
 *
 * <p>The {@link com.amazonaws.sample.entitlement.services.EntitlementService} class is responsible for:</p>
 *
 * <ol>
 *     <li>
 *         Creating a User object given an authorization string (presumably from the Authorization header of the
 *         entitlement REST request).
 *     </li>
 *     <li>
 *         Retrieving an {@link com.amazonaws.services.appstream.Application} from the AppStream service.
 *     </li>
 *     <li>
 *         Checking to see if a {@link com.amazonaws.sample.entitlement.model.User} is entitled to an AppStream
 *         {@link com.amazonaws.services.appstream.Application}
 *     </li>
 *     <li>
 *         Managing an existing AppStream {@link com.amazonaws.services.appstream.Session}.
 *     </li>
 *     <li>
 *         Entitling a {@link com.amazonaws.sample.entitlement.model.User} to an AppStream {@link com.amazonaws.services.appstream.Application}
 *         which means retrieving a new {@link com.amazonaws.services.appstream.Session} for an AppStream
 *         {@link com.amazonaws.services.appstream.Application}.
 *     </li>
 * </ol>
 */
package com.amazonaws.sample.entitlement.services;
