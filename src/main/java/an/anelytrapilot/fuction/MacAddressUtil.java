/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.fuction;

import java.net.*;
import java.util.*;

public class MacAddressUtil {
    public static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                if (network == null || !network.isUp() || network.isLoopback()) continue;

                byte[] mac = network.getHardwareAddress();
                if (mac == null) continue;

                StringBuilder sb = new StringBuilder();
                for (byte b : mac) {
                    sb.append(String.format("%02X:", b));
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);

                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }
}
