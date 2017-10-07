/* ----------------------------------------------------------------- */
/*   Appliance Agent                                                 */
/*   Takes tick rate and ResponderAgent name as input args, sends    */
/*   random energy usage request to ResponderAgent each tick that    */
/*   is between 1 - 10 kWh                                           */
/* ----------------------------------------------------------------- */

package applianceAgent;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Date;

//import org.json.*;

public class ApplianceAgent extends Agent {
	private int getComsumption() {
		return (int) Math.ceil(Math.random() * 10); 
	}
	private long CYCLE_TIME;
	private String HOME_AGENT_ADDRESS;
	private String serviceType;
	private String serviceName;
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

		// Add Ticker Behaviour, rate is input arg
		addBehaviour(new TickerBehaviour(this, CYCLE_TIME) {				
			@Override
			public void onTick() {
				log(": Sending energy usage request");

				// Create REQUEST message 
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				// Add receivers from input args
				msg.addReceiver(new AID(HOME_AGENT_ADDRESS, AID.ISLOCALNAME));
				// Set the interaction protocol
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				// Specify the reply deadline (10 seconds)
				msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
				// Set message content
				String contentJSON = "{'consumption':" + getComsumption() +",unit:'kWh'}";
				msg.setContent(contentJSON); //TODO REFACTOR OUT OF BEHAVIOUR

				// Add AchieveREInitiator behaviour 
				myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {

					// Method to handle an agree message from responder
					protected void handleAgree(ACLMessage agree) {
						log(agree.getSender().getName() + " has agreed to the request");
					}

					// Method to handle an inform message from responder
					protected void handleInform(ACLMessage inform) {
						log(inform.getSender().getName() + " successfully performed the requested action");
						log(inform.getSender().getName() + " purchased " + inform.getContent() + "kWh");
					}

					// Method to handle a refuse message from responder
					protected void handleRefuse(ACLMessage refuse) {
						log(refuse.getSender().getName() + " refused to perform the requested action");
					}

					// Method to handle a failure message (failure in delivering the message)
					protected void handleFailure(ACLMessage failure) {
						if (failure.getSender().equals(myAgent.getAMS())) {
							// FAILURE notification from the JADE runtime: the receiver (receiver does not exist)
							log("Responder does not exist");
						} else {
							log(failure.getSender().getName() + " failed to perform the requested action");
						}
					}
				});
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