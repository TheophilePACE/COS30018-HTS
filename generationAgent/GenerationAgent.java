/*
* GENERATION AGENT                                                
* Generates a consistent amount of energy in kwH and returns      
* accrued energy when asked.   
* TODO: Home agent association                                      
**/

package cos30018.assignment;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.domain.FIPANames;
import java.util.Date;

public class GenerationAgent extends Agent {
	protected void setup() {
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			System.out.println(getLocalName() + ", generation agent has been created.");
			
			// Add Ticker behaviour, rate is input arg
			addBehaviour(new TickerBehaviour(this, (long)args[0]) {				
				@Override
				public void onTick() {
					System.out.println(getBehaviourName() + ": Sending generated energy.");
					
					// Create REQUEST message 
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					// Add receivers from input args
					msg.addReceiver(new AID((String)args[1], AID.ISLOCALNAME));
					// Set the interaction protocol
					msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// Specify the reply deadline (10 seconds)
					msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					// Set message content
					/*Edit based on generation rate; remove local energy
					 * Current functionality: sends random kwH. 
					 * TODO: Local energy storage & subtract on energy send
					 **/
					msg.setContent((int) Math.ceil(Math.random() * 10) + "kWh");
					
					// Add AchieveREInitiator behaviour 
					myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
						
						// Method to handle an agree message from responder
						protected void handleAgree(ACLMessage agree) {
							System.out.println(getLocalName() + ": " + agree.getSender().getName() + " has agreed to the request");
						}
						
						// Method to handle an inform message from responder
						protected void handleInform(ACLMessage inform) {
							System.out.println(getLocalName() + ": " + inform.getSender().getName() + " successfully sent generated energy");
							System.out.println(getLocalName() + ": " + inform.getSender().getName() + " sent " + inform.getContent() + "kWh");
							System.out.println(getLocalName() + ": subtracted " + inform.getContent() + "kWh locally");
						}

						// Method to handle a refuse message from responder
						protected void handleRefuse(ACLMessage refuse) {
							System.out.println(getLocalName() + ": " + refuse.getSender().getName() + " refused energy. Energy not subtracted locally");
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
			System.out.println(getLocalName() + ": Behaviours added");
		}
		else {
			System.out.println(getLocalName() + ": Instantiation failed.");
		}	
	}
	
	protected void takeDown() {
		System.out.println(getLocalName() + ": Self destructing");
		// do cleanup
	}
}
