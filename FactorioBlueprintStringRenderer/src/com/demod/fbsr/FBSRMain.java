package com.demod.fbsr;

import java.io.IOException;

import com.demod.fbsr.cli.FBSRCommands;

public class FBSRMain {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("No command provided as program arguments.");
            FBSRCommands.interactiveShell();
            
        } else {
            FBSRCommands.execute(args);
        }
    }
}
