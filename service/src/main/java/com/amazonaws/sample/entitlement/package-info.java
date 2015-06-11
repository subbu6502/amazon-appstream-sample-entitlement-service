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
 * <p>Provides the classes necessary to start and configure the embedded Jetty web container and to configure the Spring
 * application context. The Spring Framework's Inversion of Control (IOC) capabilities are used to inject various beans used by the
 * entitlement service.</p>
 *
 * <p>Bootstrapping the service involves two classes {@link com.amazonaws.sample.entitlement.Launcher} and
 * {@link com.amazonaws.sample.entitlement.AppConfig}:</p>
 *
 * <p>{@link com.amazonaws.sample.entitlement.Launcher} is the main class of the jar and is specified as such in the jar's
 * manifest, making it possible to execute the jar via <code>java -jar sample-entitlement-service-1.0.jar</code>.</p>
 *
 * <p>{@link com.amazonaws.sample.entitlement.Launcher} starts a Jetty server with 3 servlets:</p>
 * <ol>
 *     <li>A servlet for the entitlement API '/api/*'</li>
 *     <li>A servlet for the user interface '/web/'</li>
 *     <li>A JSP servlet, also for the user interface</li>
 * </ol>
 *
 * <p>{@link com.amazonaws.sample.entitlement.AppConfig} contains the Spring application context configuration. It defines
 * various beans that are used for both the entitlement API and for the user interface. Of particular note is the
 * definition of {@link com.amazonaws.sample.entitlement.AppConfig#authorizationHandler()}. <strong>To replace the default
 * {@link com.amazonaws.sample.entitlement.authorization.AuthorizationHandler} change {@link com.amazonaws.sample.entitlement.AppConfig#authorizationHandler()}
 * to return a different or custom implementation.</strong></p>
 *
 * @see <a href="http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/spring-introduction.html">An Overview of the Spring Framework</a>
 */
package com.amazonaws.sample.entitlement;
