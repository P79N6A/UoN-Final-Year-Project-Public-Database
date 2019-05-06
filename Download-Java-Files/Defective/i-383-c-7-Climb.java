// RobotBuilder Version: 2.0
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.


package org.usfirst.frc4859.RoverTester.subsystems;


import org.usfirst.frc4859.RoverTester.commands.*;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;

// BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=IMPORTS
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=IMPORTS


/**
 *
 */
public class Climb extends Subsystem {

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTANTS

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTANTS

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DECLARATIONS
    private DoubleSolenoid kickStandSolenoid;
    private WPI_TalonSRX feetMotor;
    private DoubleSolenoid gravityShifterSolenoid;
    private AnalogInput proximitySensor;

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DECLARATIONS

    public Climb() {
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTRUCTORS
        kickStandSolenoid = new DoubleSolenoid(10, 4, 5);
        addChild("kickStandSolenoid",kickStandSolenoid);
        
        
        feetMotor = new WPI_TalonSRX(7);
        
        
        
        gravityShifterSolenoid = new DoubleSolenoid(10, 2, 3);
        addChild("gravityShifterSolenoid",gravityShifterSolenoid);
        
        
        proximitySensor = new AnalogInput(3);
        addChild("proximitySensor",proximitySensor);
        
        

    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTRUCTORS
    }

    @Override
    public void initDefaultCommand() {
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND


    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DEFAULT_COMMAND

        // Set the default command for a subsystem here.
        // setDefaultCommand(new MySpecialCommand());
    }

    @Override
    public void periodic() {
        // Put code here to be run every loop

    }

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CMDPIDGETTERS


    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CMDPIDGETTERS

    // Put methods for controlling this subsystem
    // here. Call these from Commands.
    public WPI_TalonSRX getFeetMotor(){ return feetMotor; }

    public void RunMotor(WPI_TalonSRX motor, double speed){
        motor.set(speed);
    }
    public void deploykickStand(){
        //System.out.println("deploykickStand");
        kickStandSolenoid.set(Value.kReverse);
    }

     public void retractkickstand(){
        //System.out.println("retractkickStand");
        kickStandSolenoid.set(Value.kForward);
    }
    public void deploygravityshifter(){
       //System.out.println("deploygravityshifter");
        gravityShifterSolenoid.set(Value.kForward);
    }
    public void retractgravityshifter(){
        //System.out.println("retractgravityshifter");
        gravityShifterSolenoid.set(Value.kReverse);
    }

}

