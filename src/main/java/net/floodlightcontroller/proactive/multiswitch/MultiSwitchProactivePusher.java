package net.floodlightcontroller.proactive.multiswitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.iptracker.IIpTrackerService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;

public class MultiSwitchProactivePusher 
implements IFloodlightModule {

	/*
	 * El objetivo de este ejercicio es establecer proactivamente la conectividad entre dos equipos
	 * que pueden estar conectados al mismo switch o bien a switches diferentes. Use los atributos
	 * scrIpString y dstIpString para identificar las direcciones IP
	 * */
	private String srcIpString = "10.0.0.1";
	private String dstIpString = "10.0.0.2";

	
	//TODO 1: Estos son los tres servicios que usa este módulo.
	//Por qué los necesita?
	//Cuáles métodos pueden ser útiles?
	//Cómo obtengo documentación de cada uno?
	private IRoutingService routingService;
	private IOFSwitchService switchService;
	private IIpTrackerService ipTrackerService;
	
	
	/*
	 * Puede ignorar esta información, son variables necesarias para crear las reglas
	 * */
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

	/*Aquí comienzan los métodos "burocráticos" necesarios para poder usar los servicios definidos antes*/
	
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
		l.add(IRoutingService.class);
		l.add(IDeviceService.class);
		l.add(IIpTrackerService.class);		
		l.add(IOFSwitchService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		this.routingService = context.getServiceImpl(IRoutingService.class);
		this.ipTrackerService = context.getServiceImpl(IIpTrackerService.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
	}

	
	/*
	 * Este es el método que se debe completar en este ejercicio
	 * */
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		//Estas líneas simplemente crean objetos de tipo Ipv4Address con base en las hileras definidas al inicio de la clase
		IPv4Address ipSrc = IPv4Address.of(srcIpString);
		IPv4Address ipDst = IPv4Address.of(dstIpString);
		
		
		IOFSwitch srcSwitch = null;
		IOFSwitch dstSwitch = null;
		OFPort srcPort = null;
		OFPort dstPort = null;

		
		while (srcSwitch == null || dstSwitch == null) {
			//TODO Use el servicio de rastreo de IP's para determinar el switch y el puerto
			//donde están conectados los dos dispositivos. Esta información tardará un tiempo
			//en ser descubierta, por lo que puede mantener el programa en un ciclo hasta
			//que obtenga toda la información necesaria
			
			//Importante: para que Floodlight descubra automáticamente donde está un nodo final,
			//este nodo debe tener alguna actividad en la red. Sugerencia: ejecute pingall en Mininet
			
			//srcSwitch = this.ipTrackerService.
			//dstSwitch = this.ipTrackerService.
			//srcPort = this.ipTrackerService.
			//dstPort = this.ipTrackerService.

			System.out.println("Waiting for end-points to be discovered");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//Después de ejecutar pingall, el sistema operativo descubre los dispositivos
		System.out.println("Src and Dst devices discovered. Starting to push rules");
		
		//TODO Lo único que queda por hacer es crear las reglas en los switches para
		//que los dispositivos puedan comunicarse entre sí.
		
		//Verificar si ambos dispositivos están en el mismo switch
		if (srcSwitch == dstSwitch) {
			//Cuando fuente y destino están en el mismo switch, solo debe llamar al método
			//createRulesForSwitch con los parámetros adecuados
			//this.createRulesForSwitch...
		} else { //Src y Dst están en switches diferentes
			//Cuando fuente y destino viven en switches diferentes, necesita preguntarle
			//al routingService cual es la ruta desde switch/puerto origen hasta switch/puerto destino
			Route route = null;
			//route = this.routingService.getRoute...
			List<NodePortTuple> path = route.getPath();
			//Luego, debe recorrer el path para insertar las reglas adecuadamente
			for (Iterator<NodePortTuple> iterator = path.iterator(); iterator.hasNext();) {
				NodePortTuple in = (NodePortTuple) iterator.next();
				NodePortTuple out = (NodePortTuple) iterator.next();
				IOFSwitch ofswitch = switchService.getActiveSwitch(in.getNodeId());
				//this.createRulesForSwitch..getClass().
				System.out.println("Pushing rules at switch " + ofswitch.toString());
			}
			//Al llegar a este punto, el ping entre origen y destino debería funcionar
			System.out.println("Done with multi switch rules");
		}
	}
	
	
	/*
	 * No es necesario modificar este método. Unifica en un solo lugar la funcionalidad para crear reglas en ambos sentidos
	 * en el switch. Usted únicamente debe usar este método con los parámetros adecuados: información del puerto y switch de
	 * entrada y del puerto y switch de salida, así como las direcciones IP.
	 * */
	private void createRulesForSwitch(IOFSwitch switchInstance, OFPort inPort, OFPort outPort, String srcIp, String dstIp) {
		OFFactory myFactory = OFFactories.getFactory(OFVersion.OF_10); /* Get the OFFactory version we need based on the existing object's version. */
		//Primer match, flujo en una dirección
		Match match1 = myFactory.buildMatch()
			    .setExact(MatchField.IN_PORT, inPort)
			    .setExact(MatchField.IPV4_SRC, IPv4Address.of(srcIp))
			    .setExact(MatchField.IPV4_DST, IPv4Address.of(dstIp))
			    .build();
		//Este llamado se encarga de enviar un mensaje al switch para insertar la regla
		this.writeFlowMod(switchInstance, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match1, outPort);
		
		//Segundo match, flujo en la dirección contraria
		Match match2 = myFactory.buildMatch()
			    .setExact(MatchField.IN_PORT, outPort)
			    .setExact(MatchField.IPV4_SRC, IPv4Address.of(dstIp))
			    .setExact(MatchField.IPV4_DST, IPv4Address.of(srcIp))
			    .build();
		//Este llamado se encarga de enviar un mensaje al switch para insertar la regla
		this.writeFlowMod(switchInstance, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match2, inPort);
	}
	
	//Para este ejercicio, no es necesario modificar este método. Se usa para crear las reglas
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
