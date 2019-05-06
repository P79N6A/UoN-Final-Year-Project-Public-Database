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
package org.eclipse.smarthome.config.core.dto;

/**
 * This is a data transfer object that is used to serialize filter criteria of a
 * parameter.
 *
 * @author Alex Tugarev - Initial contribution
 *
 */
public class FilterCriteriaDTO {

    public String value;
    public String name;

    public FilterCriteriaDTO() {
    }

    public FilterCriteriaDTO(String name, String value) {
        this.name = name;
        this.value = value;
    }
}