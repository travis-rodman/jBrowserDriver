/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntries;

class LogsServer extends UnicastRemoteObject implements LogsRemote, org.openqa.selenium.logging.Logs {
  private final LinkedList<Entry> entries = new LinkedList<Entry>();
  private static final LogsServer instance;
  static {
    LogsServer instanceTmp = null;
    try {
      instanceTmp = new LogsServer();
    } catch (Throwable t) {
      t.printStackTrace();
    }
    instance = instanceTmp;
  }

  static void updateSettings() {
    if (SettingsManager.settings().wireConsole()) {
      System.setProperty("org.apache.commons.logging.Log", "com.machinepublishers.jbrowserdriver.diagnostics.WireLog");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
    } else {
      System.clearProperty("org.apache.commons.logging.Log");
      System.clearProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire");
    }
  }

  static LogsServer instance() {
    return instance;
  }

  private LogsServer() throws RemoteException {}

  public void clear() {
    synchronized (entries) {
      entries.clear();
    }
  }

  public void trace(String message) {
    final Entry entry = new Entry(Level.FINEST, System.currentTimeMillis(), message);
    synchronized (entries) {
      entries.add(entry);
      if (entries.size() > SettingsManager.settings().maxLogs()) {
        entries.removeFirst();
      }
    }
    if (SettingsManager.settings().traceConsole()) {
      System.out.println(entry);
    }
  }

  public void warn(String message) {
    final Entry entry = new Entry(Level.WARNING, System.currentTimeMillis(), message);
    synchronized (entries) {
      entries.add(entry);
      if (entries.size() > SettingsManager.settings().maxLogs()) {
        entries.removeFirst();
      }
    }
    if (SettingsManager.settings().warnConsole()) {
      System.err.println(entry);
    }
  }

  public void exception(Throwable t) {
    if (t != null) {
      final Entry entry;
      StringWriter writer = null;
      try {
        writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        entry = new Entry(Level.WARNING, System.currentTimeMillis(), writer.toString());
        synchronized (entries) {
          entries.add(entry);
          if (entries.size() > SettingsManager.settings().maxLogs()) {
            entries.removeFirst();
          }
        }
      } catch (Throwable t2) {
        if (SettingsManager.settings().warnConsole()) {
          System.err.println("While logging a message, an error occurred: " + t2.getMessage());
        }
        return;
      } finally {
        Util.close(writer);
      }
      if (SettingsManager.settings().warnConsole()) {
        System.err.println(entry);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Entries getRemote(String s) {
    synchronized (entries) {
      Entries logEntries = new Entries(entries);
      entries.clear();
      return logEntries;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogEntries get(String s) {
    synchronized (entries) {
      try {
        return new Entries(entries).toLogEntries();
      } finally {
        entries.clear();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAvailableLogTypes() {
    return new HashSet<String>(Arrays.asList(new String[] { "all" }));
  }
}
