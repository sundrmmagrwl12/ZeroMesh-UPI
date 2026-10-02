package com.zeromesh.service.mesh;

import com.zeromesh.model.MeshPacket;
import com.zeromesh.model.VirtualDevice;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a Bluetooth mesh network on a single machine.
 * Manages 5 virtual devices and controls packet gossip rounds.
 */
@Service
public class MeshSimulatorService {

    private List<VirtualDevice> devices;

    // Seeds 5 virtual phones on startup — alice is the sender, bridge has internet
    @PostConstruct
    public void initNetwork() {
        devices = new ArrayList<>();
        devices.add(new VirtualDevice("phone-alice",   false));
        devices.add(new VirtualDevice("phone-bob",     false));
        devices.add(new VirtualDevice("phone-charlie", false));
        devices.add(new VirtualDevice("phone-dave",    false));
        devices.add(new VirtualDevice("phone-bridge",  true));
        System.out.println("[ZeroMesh] Network initialized with " + devices.size() + " devices.");
    }

    // Gives the initial packet to phone-alice (the payment sender)
    public void injectPacket(MeshPacket packet) {
        findDevice("phone-alice").receivePacket(packet);
        System.out.println("[ZeroMesh] Packet injected into phone-alice | TTL=" + packet.getTtl());
    }

    /**
     * Runs one Bluetooth gossip round.
     *
     * Each device with a packet tries to forward it to all other devices.
     * TTL decrements by 1 per hop — at TTL=0 the packet is held but not forwarded.
     * Forwarding is collected first then delivered to avoid ConcurrentModificationException.
     */
    public void runGossipRound() {
        System.out.println("\n[ZeroMesh] Gossip round started");

        List<ForwardIntent> forwardIntents = new ArrayList<>();

        for (VirtualDevice sender : devices) {
            for (MeshPacket packet : sender.getPackets()) {
                if (packet.isForwardable()) {
                    MeshPacket decremented = packet.withDecrementedTtl();
                    System.out.println("[" + sender.getName() + "] Forwarding | TTL: "
                            + packet.getTtl() + " → " + decremented.getTtl());
                    forwardIntents.add(new ForwardIntent(sender, decremented));
                } else {
                    System.out.println("[" + sender.getName() + "] Holding packet (TTL=0)");
                }
            }
        }

        for (ForwardIntent intent : forwardIntents) {
            for (VirtualDevice receiver : devices) {
                if (!receiver.getName().equals(intent.sender.getName())) {
                    receiver.receivePacket(intent.packet);
                }
            }
        }

        System.out.println("[ZeroMesh] Gossip round complete\n");
    }

    // Bridge device got internet — returns all held packets for server upload
    public List<MeshPacket> flushBridges() {
        List<MeshPacket> toUpload = new ArrayList<>();
        java.util.Set<String> flushedPacketIds = new java.util.HashSet<>();

        for (VirtualDevice device : devices) {
            if (device.hasInternet() && !device.getPackets().isEmpty()) {
                System.out.println("[" + device.getName() + "] Flushing "
                        + device.getPackets().size() + " packet(s) to server");
                toUpload.addAll(device.getPackets());
                for (MeshPacket p : device.getPackets()) {
                    flushedPacketIds.add(p.getPacketId());
                }
                device.clearPackets();
            }
        }

        // Once uploaded to server via bridge gateway, clear those packets from intermediate peer nodes
        // so they do not continuously re-propagate across the mesh in future gossip rounds
        if (!flushedPacketIds.isEmpty()) {
            for (VirtualDevice device : devices) {
                device.getPackets().removeIf(p -> flushedPacketIds.contains(p.getPacketId()));
            }
        }

        return toUpload;
    }

    // Clears all packets from all devices — fresh demo state
    public void resetNetwork() {
        devices.forEach(VirtualDevice::clearPackets);
        System.out.println("[ZeroMesh] Network reset.");
    }

    // Returns live status of all devices — used by dashboard
    public List<VirtualDevice> getNetworkStatus() {
        return devices;
    }

    private VirtualDevice findDevice(String name) {
        return devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Device not found: " + name));
    }

    // Holds sender + decremented packet pair to avoid modifying list during gossip iteration
    private static class ForwardIntent {
        VirtualDevice sender;
        MeshPacket packet;

        ForwardIntent(VirtualDevice sender, MeshPacket packet) {
            this.sender = sender;
            this.packet = packet;
        }
    }
}
