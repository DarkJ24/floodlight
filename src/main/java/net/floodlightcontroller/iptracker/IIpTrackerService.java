package net.floodlightcontroller.iptracker;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IIpTrackerService extends IFloodlightService {
	public IOFSwitch findAttachmentSwitchForIp(IPv4Address ipAddress);
	public OFPort findAttachmentPortForIp (IPv4Address ipAddress);
}
