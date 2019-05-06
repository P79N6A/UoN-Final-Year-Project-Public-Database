/** Generated by YAKINDU Statechart Tools code generator. */
package org.yakindu.sct.simulation.core.sexec.test;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.yakindu.sct.model.sexec.ExecutionFlow;
import org.yakindu.sct.model.sexec.interpreter.test.util.AbstractExecutionFlowTest;
import org.yakindu.sct.model.sexec.interpreter.test.util.SExecInjectionProvider;
import org.yakindu.sct.test.models.SCTUnitTestModels;
import com.google.inject.Inject;
import static org.junit.Assert.*;

/**
 * Unit TestCase for ExitOnSelfTransition
 */
@SuppressWarnings("all")
@RunWith(XtextRunner.class)
@InjectWith(SExecInjectionProvider.class)
public class ExitOnSelfTransition extends AbstractExecutionFlowTest {
	
	@Before
	public void setup() throws Exception{
		ExecutionFlow flow = models.loadExecutionFlowFromResource("ExitOnSelfTransition.sct");
		initInterpreter(flow);
	}
	@Test
	public void exitOnSelfTransitionTest() throws Exception {
		interpreter.enter();
		assertTrue(isStateActive("A"));
		assertTrue(getInteger("entryCount") == 1l);
		assertTrue(getInteger("exitCount") == 0l);
		raiseEvent("e");
		timer.timeLeap(getCyclePeriod());
		assertTrue(getInteger("entryCount") == 2l);
		assertTrue(getInteger("exitCount") == 1l);
		raiseEvent("f");
		timer.timeLeap(getCyclePeriod());
		assertTrue(getInteger("entryCount") == 2l);
		assertTrue(getInteger("exitCount") == 2l);
	}
}