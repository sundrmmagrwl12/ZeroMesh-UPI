package com.zeromesh.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single virtual phone in the Bluetooth mesh network.
 * Devices can relay packets (strangers) or upload them when they have internet (bridge).
 */
public class VirtualDevice {

    private String name;
    private boolean hasInternet;
    private List<MeshPacket> packets;

    public VirtualDevice(String name, boolean hasInternet) {
        this.name        = name;
        this.hasInternet = hasInternet;
        this.packets     = new ArrayList<>();
    }

    // Adds packet only if not already held — avoids storing same packet twice on same device
    public void receivePacket(MeshPacket packet) {
        boolean alreadyHave = packets.stream()
                .anyMatch(p -> p.getPacketId().equals(packet.getPacketId()));
        if (!alreadyHave) {
            packets.add(packet);
            System.out.println("[" + name + "] Received: " + packet.getPacketId()
                    + " | TTL remaining: " + packet.getTtl());
        }
    }

    // Clears device memory after bridge uploads packets to server
    public void clearPackets() {
        packets.clear();
    }

    public String getName()              { return name; }
    public boolean hasInternet()         { return hasInternet; }
    public void setHasInternet(boolean v){ this.hasInternet = v; }
    public List<MeshPacket> getPackets() { return packets; }
}
