package net.floodlightcontroller.proactive.singleswitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class SingleSwitchProactivePusher 
implements IFloodlightModule {

	//Vamos a usar el servicio de switches de Floodlight
	private IOFSwitchService switchService;
	
	// flow-mod - for use in the cookie
	public static final int LEARNING_SWITCH_APP_ID = 1;
	// LOOK! This should probably go in some class that encapsulates
	// the app cookie management
	public static final int APP_ID_BITS = 12;
	public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	public static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;

	// more flow-mod defaults
	private static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0; // in seconds
	private static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	private static short FLOWMOD_PRIORITY = 100;

	// for managing our map sizes
	protected static final int MAX_MACS_PER_SWITCH  = 1000;

	// normally, setup reverse flow as well. Disable only for using cbench for comparison with NOX etc.
	protected static final boolean LEARNING_SWITCH_REVERSE_FLOW = true;

	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		
		//Observe como se usa el servicio de switches del sistema operativo para obtener
		//una referencia al set de dispositivos detectados en este momento
		//En una topología con 3 switches, este set tendrá 3 elementos
		Set<DatapathId> switches = this.switchService.getAllSwitchDpids();
		
		/*
		 * TODO #1 - Encicle el programa para que duerma por 5 segundos hasta que haya al menos un switch en el set switches
		 * */
		/*
		 * while(switches vacío) {
		 * 		dormir 5 segundos
		 * }
		 */
		while(switches.isEmpty()) {
			switches = this.switchService.getAllSwitchDpids();
			try {
				Thread.sleep(5000);
				System.out.println("Durmiendo");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//TODO #2
		//Al salir del ciclo, podemos suponer que existe al menos un switch
		//En este ejercicio asumimos que hay exactamente un switch, por lo que extraemos el primero
		DatapathId onlySwitch = switches.iterator().next();
		IOFSwitch switchInstance = switchService.getActiveSwitch(onlySwitch);
		System.out.println("Se conectó el switch con ID " + onlySwitch.toString());
		
		//TODO #3
		//Ahora debemos crear dos reglas: una para cada dirección del flujo
		//El match que se muestra a continuación es incorrecto (no todos los datos calzan)
		//Cuál es el match con menos campos que se puede crear?
		//Cuál es el match con más campos que se puede crear?
		OFFactory myFactory = OFFactories.getFactory(OFVersion.OF_13); /* Get the OFFactory version we need based on the existing object's version. */
		Match match1 = myFactory.buildMatch()
			    .setExact(MatchField.IN_PORT, OFPort.of(1))
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1/24"))
//			    .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
//		    	.setExact(MatchField.TCP_DST, TransportPort.of(80))
			    .build();
		//Este llamado se encarga de enviar un mensaje al switch para insertar la regla
		this.writeFlowMod(switchInstance, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match1, OFPort.of(2));


		//Este llamado se encarga de enviar un mensaje al switch para insertar la regla
		Match match2 = myFactory.buildMatch()
			    .setExact(MatchField.IN_PORT, OFPort.of(2))
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1/24"))
//			    .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
	//		    .setExact(MatchField.TCP_DST, TransportPort.of(80))
			    .build();
		
		//Este llamado se encarga de enviar un mensaje al switch para insertar la segunda regla
		this.writeFlowMod(switchInstance, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match2, OFPort.of(1));
		
		System.out.println("Listo! Intente ejecutar h1 ping h2 en la consola de Mininet. El ping debería funcionar.");
	}
	
	//Para este ejercicio, no es necesario modificar este método
	/**
	 * Writes a OFFlowMod to a switch.
	 * @param sw The switch tow rite the flowmod to.
	 * @param command The FlowMod actions (add, delete, etc).
	 * @param bufferId The buffer ID if the switch has buffered the packet.
	 * @param match The OFMatch structure to write.
	 * @param outPort The switch port to output it to.
	 */
	private void writeFlowMod(IOFSwitch sw, OFFlowModCommand command, OFBufferId bufferId,
			Match match, OFPort outPort) {
		OFFlowMod.Builder fmb;
		if (command == OFFlowModCommand.DELETE) {
			fmb = sw.getOFFactory().buildFlowDelete();
		} else {
			fmb = sw.getOFFactory().buildFlowAdd();
		}
		fmb.setMatch(match);
		fmb.setCookie((U64.of(LEARNING_SWITCH_COOKIE)));
		fmb.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT);
		fmb.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT);
		fmb.setPriority(FLOWMOD_PRIORITY);
		fmb.setBufferId(bufferId);
		fmb.setOutPort((command == OFFlowModCommand.DELETE) ? OFPort.ANY : outPort);
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		if (command != OFFlowModCommand.DELETE) {
			sfmf.add(OFFlowModFlags.SEND_FLOW_REM);
		}
		fmb.setFlags(sfmf);

		List<OFAction> al = new ArrayList<OFAction>();
		al.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
		fmb.setActions(al);

		// and write it out
		sw.write(fmb.build());
	}
	
	

}
