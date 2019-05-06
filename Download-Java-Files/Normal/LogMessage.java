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
package org.eclipse.che.infrastructure.docker.client;

/** @author andrew00x */
public class LogMessage {
  public enum Type {
    STDIN,
    STDOUT,
    STDERR,
    DOCKER
  }

  private final Type type;
  private final String content;

  public LogMessage(Type type, String content) {
    this.type = type;
    this.content = content;
  }

  public Type getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "LogMessage{" + "type=" + type + ", content='" + content + '\'' + '}';
  }
}
