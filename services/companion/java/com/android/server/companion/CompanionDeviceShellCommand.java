/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.android.server.companion;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.companion.AssociationInfo;
import android.os.ShellCommand;
import android.util.Base64;

import com.android.server.companion.securechannel.CompanionSecureCommunicationsManager;

import com.android.server.companion.presence.CompanionDevicePresenceMonitor;

import java.io.PrintWriter;
import java.util.List;

class CompanionDeviceShellCommand extends ShellCommand {
    private static final String TAG = "CompanionDevice_ShellCommand";

    private final CompanionDeviceManagerService mService;
    private final AssociationStore mAssociationStore;
    private final CompanionSecureCommunicationsManager mSecureCommsManager;
    private final CompanionDevicePresenceMonitor mDevicePresenceMonitor;

    CompanionDeviceShellCommand(CompanionDeviceManagerService service,
            AssociationStore associationStore,
            CompanionSecureCommunicationsManager secureCommsManager,
            CompanionDevicePresenceMonitor devicePresenceMonitor) {
        mService = service;
        mAssociationStore = associationStore;
        mSecureCommsManager = secureCommsManager;
        mDevicePresenceMonitor = devicePresenceMonitor;
    }

    @Override
    public int onCommand(String cmd) {
        final PrintWriter out = getOutPrintWriter();
        final int associationId;
        try {
            switch (cmd) {
                case "list": {
                    final int userId = getNextIntArgRequired();
                    final List<AssociationInfo> associationsForUser =
                            mAssociationStore.getAssociationsForUser(userId);
                    for (AssociationInfo association : associationsForUser) {
                        // TODO(b/212535524): use AssociationInfo.toShortString(), once it's not
                        //  longer referenced in tests.
                        out.println(association.getPackageName() + " "
                                + association.getDeviceMacAddress());
                    }
                }
                break;

                case "associate": {
                    int userId = getNextIntArgRequired();
                    String packageName = getNextArgRequired();
                    String address = getNextArgRequired();
                    mService.legacyCreateAssociation(userId, address, packageName, null);
                }
                break;

                case "disassociate": {
                    final int userId = getNextIntArgRequired();
                    final String packageName = getNextArgRequired();
                    final String address = getNextArgRequired();
                    final AssociationInfo association =
                            mService.getAssociationWithCallerChecks(userId, packageName, address);
                    if (association != null) {
                        mService.disassociateInternal(association.getId());
                    }
                }
                break;

                case "clear-association-memory-cache":
                    mService.persistState();
                    mService.loadAssociationsFromDisk();
                    break;

                case "send-secure-message":
                    associationId = getNextIntArgRequired();
                    final byte[] message;

                    // The message should be either a UTF-8 String or Base64-encoded data.
                    final boolean isBase64 = "--base64".equals(getNextOption());
                    if (isBase64) {
                        final String base64encodedMessage = getNextArgRequired();
                        message = Base64.decode(base64encodedMessage, 0);
                    } else {
                        // We treat the rest of the command as the message, which should contain at
                        // least one word (hence getNextArg_Required() below), but there may be
                        // more.
                        final StringBuilder sb = new StringBuilder(getNextArgRequired());
                        // Pick up the rest.
                        for (String word : peekRemainingArgs()) {
                            sb.append(" ").append(word);
                        }
                        // And now convert to byte[]...
                        message = sb.toString().getBytes(UTF_8);
                    }

                    mSecureCommsManager.sendSecureMessage(associationId, /* messageId */ 0,
                            message);
                    break;

                case "simulate-device-appeared":
                    associationId = getNextIntArgRequired();
                    mDevicePresenceMonitor.simulateDeviceAppeared(associationId);
                    break;

                case "simulate-device-disappeared":
                    associationId = getNextIntArgRequired();
                    mDevicePresenceMonitor.simulateDeviceDisappeared(associationId);
                    break;

                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Throwable e) {
            final PrintWriter errOut = getErrPrintWriter();
            errOut.println();
            errOut.println("Exception occurred while executing '" + cmd + "':");
            e.printStackTrace(errOut);
            return 1;
        }
        return 0;
    }

    private int getNextIntArgRequired() {
        return Integer.parseInt(getNextArgRequired());
    }

    @Override
    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Companion Device Manager (companiondevice) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  list USER_ID");
        pw.println("      List all Associations for a user.");
        pw.println("  associate USER_ID PACKAGE MAC_ADDRESS");
        pw.println("      Create a new Association.");
        pw.println("  disassociate USER_ID PACKAGE MAC_ADDRESS");
        pw.println("      Remove an existing Association.");
        pw.println("  send-secure-message ASSOCIATION_ID [--base64] MESSAGE");
        pw.println("      Send a secure message to an associated companion device.");
        pw.println("  clear-association-memory-cache");
        pw.println("      Clear the in-memory association cache and reload all association ");
        pw.println("      information from persistent storage. USE FOR DEBUGGING PURPOSES ONLY.");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");

        pw.println("  simulate-device-appeared ASSOCIATION_ID");
        pw.println("      Make CDM act as if the given companion device has appeared.");
        pw.println("      I.e. bind the associated companion application's");
        pw.println("      CompanionDeviceService(s) and trigger onDeviceAppeared() callback.");
        pw.println("      The CDM will consider the devices as present for 60 seconds and then");
        pw.println("      will act as if device disappeared, unless 'simulate-device-disappeared'");
        pw.println("      or 'simulate-device-appeared' is called again before 60 seconds run out"
                + ".");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");

        pw.println("  simulate-device-disappeared ASSOCIATION_ID");
        pw.println("      Make CDM act as if the given companion device has disappeared.");
        pw.println("      I.e. unbind the associated companion application's");
        pw.println("      CompanionDeviceService(s) and trigger onDeviceDisappeared() callback.");
        pw.println("      NOTE: This will only have effect if 'simulate-device-appeared' was");
        pw.println("      invoked for the same device (same ASSOCIATION_ID) no longer than");
        pw.println("      60 seconds ago.");
        pw.println("      USE FOR DEBUGGING AND/OR TESTING PURPOSES ONLY.");
    }
}
