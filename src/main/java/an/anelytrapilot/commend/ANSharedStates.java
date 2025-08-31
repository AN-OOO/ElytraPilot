/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.commend;

import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.nio.file.Path;

public class ANSharedStates {
    public static boolean shouldStartWalking = false;
    public static BlockPos walkTarget = null;

    public static boolean FlyCommend = false;
    public static boolean OverWorldFlyCommend = false;
    public static boolean TestCommend = false;
    public static BlockPos FlyTarget = null;

    public static boolean FlyStop = false;

    public static boolean FileLoad = false;

    public static Path file;

}
