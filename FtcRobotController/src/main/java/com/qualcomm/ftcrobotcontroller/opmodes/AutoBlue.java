package com.qualcomm.ftcrobotcontroller.opmodes;

import com.qualcomm.hardware.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;
import com.qualcomm.robotcore.robocol.Telemetry;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

public class AutoBlue extends BotHardware 
{
    private short state = 0;
    private short close, far, good;

    @Override
    public void runOpMode() throws InterruptedException 
    {
        super.runOpMode();
        close = far = good = 0;
        initHardware();

        while (gyro.isCalibrating()) 
        {
            Thread.sleep(50);
        }

        waitForStart();

        gyro.resetZAxisIntegrator();

        setTime();

        while (opModeIsActive()) 
        {
            telemetry.addData("State", state);
            telemetry.addData("Z", gyro.getIntegratedZValue());
            telemetry.addData("Beacon Red", beacon.red());
            telemetry.addData("Beacon Blue", beacon.blue());
            telemetry.addData("Sonic Dist", sonar.getUltrasonicLevel());
            telemetry.addData("Time: ", getTime());
            telemetry.addData("Color:", groundLeft.blue());

            switch (state) 
            {
                case 0://start drive
                    setPower(0.25f);
                    state++;
                    break;
                case 1: //after 1 sec stop to turn
                    if(getTime() > 1.4) // auto red had only 1 sec; should this be 1.4 or 1???
                    {
                        setPower(0);
                        state++;
                    }
                    break;
                case 2: //first turn 45 degrees //CHANGE (auto red was opp for the negatives)
                    setPower(-0.35f, 0.35f);
                    if(Math.abs(gyro.getIntegratedZValue()) > 44)
                    {
                        setPower(0);
                        state++;
                    }
                    break;
                case 3: //reset gyro
                    gyro.resetZAxisIntegrator();
                    state++;
                    break;
                case 4: //drive until white line
                    driveGyro(0.3f);
                    if(getTime() > 10)
                    {
                        setPower(0);
                        state = 100;
                        break;
                    }
                    if(isLeftOnLine())
                    {
                        state ++;
                        setTime();
                    }
                    break;
                case 5: //turn
                    if(getTime() > 0.2) 
                    {
                        gyro.resetZAxisIntegrator();
                        setPower(-0.35f, 0.35f); //auto red is opposite negative signs
                        state++;
                    }
                    break;
                case 6: //getting into proper position on white line
                    if(Math.abs(gyro.getIntegratedZValue()) > 55)
                    {
                        setPower(0.35f, -0.35f); //auto red had opp negative signs
                    } 
                    else if(Math.abs(gyro.getIntegratedZValue()) > 44)
                    {
                        setPower(0f);
                        gyro.resetZAxisIntegrator();
                        state++;
                    }
                    break;
                case 7: //drive forward until good distance for measuring color of beacon
                    if(sonar.getUltrasonicLevel() < 21) 
                    {
                        far = good = 0;
                        close++;
                    } 
                    else if (sonar.getUltrasonicLevel() > 24) 
                    {
                        close = good = 0;
                        far++;
                    } 
                    else 
                    {
                        close = far = 0;
                        good++;
                    }

                    if (close == 2) 
                    {
                        setPower(-0.1f);
                        close = 0;
                    } 
                    else if (far == 2) 
                    {
                        setPower(0.1f);
                        far = 0;
                    } 
                    else if (good == 2) 
                    {
                        setPower(0);
                        thrower.setPosition(1);
                        state++;
                        close = far = good = 0;
                    }
                    break;
                case 8: //beacon pusher
                    try 
                    {
                        Thread.sleep(2500);
                    }
                    catch (InterruptedException e)
                    {
                        telemetry.addData("ERROR", e.getStackTrace()[0]);
                    }
                    if(beacon.red() < 5 && beacon.blue() < 5)
                    {
                        beaconServo.setPosition(0.1);
                        telemetry.addData("Beacon", "FAILED");
                        state = 100;
                        break;
                    }
                    if(beacon.red() > beacon.blue())
                    {
                        telemetry.addData("RED color: ", beacon.red());
                        telemetry.addData("BLUE color: ", beacon.blue());
                        try {
                            Thread.sleep(2000);
                        }catch (InterruptedException e){
                            telemetry.addData("ERROR", e.getStackTrace()[0]);
                        }
                        beaconServo.setPosition(1);
                    } 
                    else 
                    {
                        telemetry.addData("BLUE color: ", beacon.blue());
                        telemetry.addData("RED color: ", beacon.red());
                        try 
                        {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e)
                        {
                            telemetry.addData("ERROR", e.getStackTrace()[0]);
                        }
                        beaconServo.setPosition(1); //changed to 1, auto red is zero
                    }
                    try 
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        telemetry.addData("ERROR", e.getStackTrace()[0]);
                    }
                    state++;
                    setPower(0.35f);
                    break;
                case 9: //drive forward until push beacon
                    try 
                    {
                        Thread.sleep(1250);
                    }
                    catch (InterruptedException e)
                    {
                        telemetry.addData("ERROR", e.getStackTrace()[0]);
                    }
                    setPower(0);
                    state++;
                    break;
                
                
                //Unshare about further edits from this point
                case 10: //rotate 90 degrees
                     gyro.resetZAxisIntegrator();
                     setPower(0.35f, -0.35f);
                     if(Math.abs(gyro.getIntegratedZValue()) < 89)
                     {
                        setPower(0);
                        state++;
                     }
                     break;
               case 11: //drive forward until white line
                    gyro.resetZAxisIntegrator();
                    driveGyro(0.3f);
                    if(getTime() > 10)
                    {
                        setPower(0);
                        state = 100;
                        break;
                    }
                    if(isLeftOnLine())
                    {
                        state++;
                        setTime();
                    }
                    break;
               case 12: //rotate 90 degrees to face beacon again -- check direction of turn
                     gyro.resetZAxisIntegrator();
                     setPower(0.35f, -0.35f);
                     if(Math.abs(gyro.getIntegratedZValue()) < 89)
                     {
                        setPower(0);
                        state++;
                     }
                     break;
               case 13: //push beacon -- check code
                    try 
                    {
                        Thread.sleep(1250);
                    }
                    catch (InterruptedException e)
                    {
                        telemetry.addData("ERROR", e.getStackTrace()[0]);
                    }
                    setPower(0);
                    state++;
                    break;
               case 14: //drive backwards to center
               default:
                    setPower(0);
                    break;
            }
            waitOneFullHardwareCycle();
        }
    }
}
