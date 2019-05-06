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
package org.eclipse.smarthome.core.storage;

/**
 * The {@link DeletableStorageService} extends the normal {@link StorageService} and provides instances of
 * {@link DeletableStorage}s.
 *
 * @author Markus Rathgeb - Initial Contribution and API
 */
public interface DeletableStorageService extends StorageService {

    @Override
    <T> DeletableStorage<T> getStorage(String name);

    @Override
    <T> DeletableStorage<T> getStorage(String name, ClassLoader classLoader);
}
