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
package org.eclipse.che.ide.api.workspace.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when ws-agent server in some machine goes into a running state. Allows to avoid checking
 * server's name unlike {@link ServerRunningEvent}.
 *
 * @see ServerRunningEvent
 */
public class WsAgentServerRunningEvent extends GwtEvent<WsAgentServerRunningEvent.Handler> {

  public static final Type<WsAgentServerRunningEvent.Handler> TYPE = new Type<>();

  private final String machineName;

  public WsAgentServerRunningEvent(String machineName) {
    this.machineName = machineName;
  }

  /** Returns the related machine's name. */
  public String getMachineName() {
    return machineName;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onWsAgentServerRunning(this);
  }

  public interface Handler extends EventHandler {
    void onWsAgentServerRunning(WsAgentServerRunningEvent event);
  }
}
