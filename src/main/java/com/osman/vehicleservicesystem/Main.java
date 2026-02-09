package com.osman.vehicleservicesystem;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppUI().setVisible(true));
    }
}
