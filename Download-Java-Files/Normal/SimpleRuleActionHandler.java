/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.automation.module.script.rulesupport.shared.simple;

import java.util.Map;

import org.openhab.core.automation.Action;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
public interface SimpleRuleActionHandler {
    Object execute(Action module, Map<String, ?> inputs);
}
