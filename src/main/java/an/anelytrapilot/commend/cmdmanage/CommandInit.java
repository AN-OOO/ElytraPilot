/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.commend.cmdmanage;

public class CommandInit {

    public final static CommandManager ANCOMMAND = new CommandManager();

    public static void tick(){

        ANCOMMAND.setPrefix("@");


    }
}
