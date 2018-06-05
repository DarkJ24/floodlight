package net.floodlightcontroller.iptracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.learningswitch.ILearningSwitchService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

public class IpTracker implements IIpTrackerService, IOFMessageListener, IFloodlightModule {

	private Hashtable<IPv4Address, IOFSwitch> ipToSwitch;
	private Hashtable<IPv4Address, OFPort> ipToPort;
	private IFloodlightProviderService floodlightProviderService;


	@Override
	public String getName() {
		return "IP Tracker";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		if (msg.getType() == OFType.PACKET_IN) {
			OFPacketIn pi = (OFPacketIn) msg;
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			IPv4Address IPv4SrcAddr = this.getIPv4Address(eth);
			if (this.ipToSwitch.get(IPv4SrcAddr) == null) {
				this.ipToSwitch.put(IPv4SrcAddr, sw);
				this.ipToPort.put(IPv4SrcAddr, pi.getMatch().get(MatchField.IN_PORT));
				System.out.println("IP " + IPv4SrcAddr.toString() + " attached to switch " + sw.toString() + " port = " + pi.getInPort().toString());
			}
		}	
		return Command.CONTINUE;
	}

	private IPv4Address getIPv4Address(Ethernet eth) {
		IPv4Address IPv4SrcAddr = null;
		if (eth.getEtherType() == EthType.IPv4) {
			IPv4 IPv4Payload = (IPv4) eth.getPayload();
			IPv4SrcAddr = IPv4Payload.getSourceAddress();
		} else if (eth.getEtherType() == EthType.ARP) {
			ARP arpPayload = (ARP) eth.getPayload();
			IPv4SrcAddr = arpPayload.getSenderProtocolAddress();			
		}
		return IPv4SrcAddr;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IIpTrackerService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>,  IFloodlightService> m = 
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IIpTrackerService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		this.ipToSwitch = new Hashtable<>();
		this.ipToPort = new Hashtable<>();
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		this.ipToSwitch = new Hashtable<>();
		this.ipToPort = new Hashtable<>();
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public IOFSwitch findAttachmentSwitchForIp(IPv4Address ipAddress){
		return this.ipToSwitch.get(ipAddress);
	}

	@Override
	public OFPort findAttachmentPortForIp(IPv4Address ipAddress) {
		return this.ipToPort.get(ipAddress);
	}

}
