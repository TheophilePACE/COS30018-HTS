/* ----------------------------------------------------------------- */
/*   Appliance Agent                                                 */
/*   Takes tick rate and ResponderAgent name as input args, sends    */
/*   random energy usage request to ResponderAgent each tick that    */
/*   is between 1 - 10 kWh                                           */
/* ----------------------------------------------------------------- */

package applianceAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;

//import org.json.*;

public class ApplianceAgent extends Agent {
	private int getComsumption() {
		return (int) Math.ceil(Math.random() * 10); 
	}
	private long CYCLE_TIME;
	private String HOME_AGENT_ADDRESS;
	private String serviceType;
	private String serviceName;
	private MessageTemplate energyBalanceMessageTemplate;

	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Appliance AGent needs arguments!!!");
		}

		CYCLE_TIME = (long)args[0];
		HOME_AGENT_ADDRESS = args[1].toString();
		serviceType = args[2].toString();
		serviceName = args[3].toString();

		registerService(serviceType, serviceName);
		log("created: "+serviceName+" -> "+serviceName);

		//tmplate for a Message type resuest from the homeagent
		energyBalanceMessageTemplate = MessageTemplate.and( MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST)),
				MessageTemplate.MatchSender(new AID(HOME_AGENT_ADDRESS,AID.ISLOCALNAME)) );

		// Add Ticker Behaviour, rate is input arg
		addBehaviour(new AchieveREResponder(this, energyBalanceMessageTemplate) {				
			@Override
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println("Agent "+getLocalName()+": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
				// We agree to perform the action. Note that in the FIPA-Request
				// protocol the AGREE message is optional. Return null if you
				// don't want to send it.
				System.out.println("Agent "+getLocalName()+": Agree");
				ACLMessage consumptionMessageResponse = request.createReply();
				consumptionMessageResponse.setPerformative(ACLMessage.INFORM);
				String contentJSON = "{'consumption':" + getComsumption() +",unit:'kWh'}";
				consumptionMessageResponse.setContent(contentJSON); //TODO REFACTOR OUT OF BEHAVIOUR
				return consumptionMessageResponse;
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
				System.out.println("Agent "+getLocalName()+": Action successfully performed");
				ACLMessage inform = request.createReply();
				inform.setPerformative(ACLMessage.INFORM);
				return inform;
			}
		});
		log("I have added my behaviours");
	}
	

	// Method to register the service
	void registerService(String type,String name)
	{
		ServiceDescription sd  = new ServiceDescription();
		sd.setType(type);
		sd.setName(name);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addServices(sd); 

		try {  
			DFService.register(this, dfd );  
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
	}

	protected void takeDown() {
		log("Preparing to die");
		// do cleanup
	}
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}