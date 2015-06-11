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
 * <p>Provides the Java API for RESTful Web Services (JAX-RS) endpoint for the entitlement service.</p>
 *
 * <table cellspacing="0">
 *     <thead>
 *         <tr class="altColor"><th class="colFirst">Endpoint</th><th class="colLast">Description</th></tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td class="colFirst">/entitlement/{applicationId}</td>
 *             <td class="colLast">
 *                 <p>Request the user specified in the Authorization header be entitled to the application represented
 *                 by the application id.</p>
 *                 <p>Returns one of the following:</p>
 *                 <table cellspacing="0">
 *                     <thead>
 *                         <tr>
 *                             <th>HTTP Response</th>
 *                             <th>Description</th>
 *                         </tr>
 *                     </thead>
 *                     <tbody>
 *                         <tr class="altColor">
 *                             <td>201 (Created)</td><td>Entitlement URL. E.g. https://appstream-alpha.us-east-1.amazonaws.com/entitlements/c621126c-de9e-482c-ba5a-5b467dacc5c7</td>
 *                         </tr>
 *                         <tr>
 *                             <td>401 (Unauthorized)</td>
 *                             <td>
 *                                 Will be returned if the Authorization header is missing or invalid or the user provided
 *                                 in the header failed authorization.
 *                             </td>
 *                         </tr>
 *                         <tr class="altColor">
 *                             <td>403 (Forbidden)</td>
 *                             <td>
 *                                 Will be returned if authorization succeeded but the user is not entitled to the application.
 *                             </td>
 *                         </tr>
 *                         <tr>
 *                             <td>404 (Not Found)</td>
 *                             <td>
 *                                 Will be returned if the application corresponding to the provided applicationId could
 *                                 not be found.
 *                             </td>
 *                         </tr>
 *                         <tr>
 *                             <td>409 (Conflict)</td>
 *                             <td>
 *                                 Will be returned if the application corresponding to the provided applicationId is in
 *                                 an error state.
 *                             </td>
 *                         </tr>
 *                     </tbody>
 *                 </table>
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @see <a href="http://cxf.apache.org/docs/jax-rs.html">Apache CXF JAX-RS (JSR-311)</a>
 */
package com.amazonaws.sample.entitlement.rs;
