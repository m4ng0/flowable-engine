/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.idm.engine.delegate.event.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.activiti.idm.api.event.ActivitiIdmEvent;
import org.activiti.idm.api.event.ActivitiIdmEventListener;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.engine.ActivitiIdmException;
import org.activiti.idm.engine.ActivitiIdmIllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that allows adding and removing event listeners and dispatching events to the appropriate listeners.
 * 
 * @author Tijs Rademakers
 */
public class ActivitiIdmEventSupport {

  private static final Logger LOG = LoggerFactory.getLogger(ActivitiIdmEventSupport.class);

  protected List<ActivitiIdmEventListener> eventListeners;
  protected Map<ActivitiIdmEventType, List<ActivitiIdmEventListener>> typedListeners;

  public ActivitiIdmEventSupport() {
    eventListeners = new CopyOnWriteArrayList<ActivitiIdmEventListener>();
    typedListeners = new HashMap<ActivitiIdmEventType, List<ActivitiIdmEventListener>>();
  }

  public synchronized void addEventListener(ActivitiIdmEventListener listenerToAdd) {
    if (listenerToAdd == null) {
      throw new ActivitiIdmIllegalArgumentException("Listener cannot be null.");
    }
    if (!eventListeners.contains(listenerToAdd)) {
      eventListeners.add(listenerToAdd);
    }
  }

  public synchronized void addEventListener(ActivitiIdmEventListener listenerToAdd, ActivitiIdmEventType... types) {
    if (listenerToAdd == null) {
      throw new ActivitiIdmIllegalArgumentException("Listener cannot be null.");
    }

    if (types == null || types.length == 0) {
      addEventListener(listenerToAdd);

    } else {
      for (ActivitiIdmEventType type : types) {
        addTypedEventListener(listenerToAdd, type);
      }
    }
  }

  public void removeEventListener(ActivitiIdmEventListener listenerToRemove) {
    eventListeners.remove(listenerToRemove);

    for (List<ActivitiIdmEventListener> listeners : typedListeners.values()) {
      listeners.remove(listenerToRemove);
    }
  }

  public void dispatchEvent(ActivitiIdmEvent event) {
    if (event == null) {
      throw new ActivitiIdmIllegalArgumentException("Event cannot be null.");
    }

    if (event.getType() == null) {
      throw new ActivitiIdmIllegalArgumentException("Event type cannot be null.");
    }

    // Call global listeners
    if (!eventListeners.isEmpty()) {
      for (ActivitiIdmEventListener listener : eventListeners) {
        dispatchEvent(event, listener);
      }
    }

    // Call typed listeners, if any
    List<ActivitiIdmEventListener> typed = typedListeners.get(event.getType());
    if (typed != null && !typed.isEmpty()) {
      for (ActivitiIdmEventListener listener : typed) {
        dispatchEvent(event, listener);
      }
    }
  }

  protected void dispatchEvent(ActivitiIdmEvent event, ActivitiIdmEventListener listener) {
    try {
      listener.onEvent(event);
    } catch (Throwable t) {
      if (listener.isFailOnException()) {
        throw new ActivitiIdmException("Exception while executing event-listener", t);
      } else {
        // Ignore the exception and continue notifying remaining listeners. The listener
        // explicitly states that the exception should not bubble up
        LOG.warn("Exception while executing event-listener, which was ignored", t);
      }
    }
  }

  protected synchronized void addTypedEventListener(ActivitiIdmEventListener listener, ActivitiIdmEventType type) {
    List<ActivitiIdmEventListener> listeners = typedListeners.get(type);
    if (listeners == null) {
      // Add an empty list of listeners for this type
      listeners = new CopyOnWriteArrayList<ActivitiIdmEventListener>();
      typedListeners.put(type, listeners);
    }

    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }
}
