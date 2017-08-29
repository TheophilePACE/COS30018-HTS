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
import jade.domain.FIPANames;
import java.util.Date;

public class ApplianceAgent extends Agent {
	protected void setup() {
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			System.out.println(getLocalName() + ": I have been created");
			
			// Add Ticker Behaviour, rate is input arg
			addBehaviour(new TickerBehaviour(this, (long)args[0]) {				
				@Override
				public void onTick() {
					System.out.println(getBehaviourName() + ": Sending energy usage request");
					
					// Create REQUEST message 
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					// Add receivers from input args
					msg.addReceiver(new AID((String)args[1], AID.ISLOCALNAME));
					// Set the interaction protocol
					msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// Specify the reply deadline (10 seconds)
					msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					// Set message content
					msg.setContent((int) Math.ceil(Math.random() * 10) + "kWh");
					
					// Add AchieveREInitiator behaviour 
					myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
						
						// Method to handle an agree message from responder
						protected void handleAgree(ACLMessage agree) {
							System.out.println(getLocalName() + ": " + agree.getSender().getName() + " has agreed to the request");
						}
						
						// Method to handle an inform message from responder
						protected void handleInform(ACLMessage inform) {
							System.out.println(getLocalName() + ": " + inform.getSender().getName() + " successfully performed the requested action");
							System.out.println(getLocalName() + ": " + inform.getSender().getName() + " purchased " + inform.getContent() + "kWh");
						}

						// Method to handle a refuse message from responder
						protected void handleRefuse(ACLMessage refuse) {
							System.out.println(getLocalName() + ": " + refuse.getSender().getName() + " refused to perform the requested action");
						}

						// Method to handle a failure message (failure in delivering the message)
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// FAILURE notification from the JADE runtime: the receiver (receiver does not exist)
								System.out.println(getLocalName() + ": " + "Responder does not exist");
							} else {
								System.out.println(getLocalName() + ": " + failure.getSender().getName() + " failed to perform the requested action");
							}
						}
					});
				}
			});
			System.out.println(getLocalName() + ": I have added my behaviours");
		}
		else {
			System.out.println(getLocalName() + ": Instantiation failed.");
		}	
	}
	
	protected void takeDown() {
		System.out.println(getLocalName() + ": Preparing to die");
		// do cleanup
	}
}
