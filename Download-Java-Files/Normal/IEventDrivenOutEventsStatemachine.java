/** Generated by YAKINDU Statechart Tools code generator. */
package org.yakindu.scr.eventdrivenoutevents;

import org.yakindu.scr.IStatemachine;

public interface IEventDrivenOutEventsStatemachine extends IStatemachine {
	public interface SCInterface {
	
		public void raiseE1();
		
		public boolean isRaisedE2();
		
	}
	
	public SCInterface getSCInterface();
	
}
