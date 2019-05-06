/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.project.shared;

import org.eclipse.che.api.project.shared.dto.event.FileWatcherEventType;

/**
 * Entity which describes change occurred to the specific path.
 *
 * @author Vlad Zhukovskyi
 * @since 5.19.0
 */
public interface FileChange {
  String getPath();

  boolean isFile();

  FileWatcherEventType getType();
}
