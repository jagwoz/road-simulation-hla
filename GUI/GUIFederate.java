/*
 *   Copyright 2012 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *   
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package GUI;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GUIFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private GUIFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	//car federate
	protected ObjectClassHandle carHandle;
	protected AttributeHandle carPositionsHandle;

	//road federate
	protected ObjectClassHandle roadHandle;
	protected AttributeHandle roadPreparedHandle;
	protected InteractionClassHandle roadPrepareStartHandle;

	//gui federate
	protected ObjectClassHandle GUIHandle;
	protected AttributeHandle GUIGuiHandle;

	protected ArrayList<Integer> carsPosition;
	public boolean isTimeGo;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "GUIFederate   : " + message );
	}

	private void waitForUser()
	{
		log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
		BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
		try
		{
			reader.readLine();
		}
		catch( Exception e )
		{
			log( "Error while waiting for user input: " + e.getMessage() );
			e.printStackTrace();
		}
	}

	//----------------------------------------------------------
	//                 MAIN SIMULATION METHODS
	//----------------------------------------------------------
	public void runFederate( String federateName ) throws Exception
	{
		GUI.getInstance().addPanel();

		log( "Creating RTIambassador" );
		rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

		log( "Connecting..." );
		fedAmb = new GUIFederateAmbassador( this );
		rtiAmb.connect(fedAmb, CallbackModel.HLA_EVOKED );

		log( "Creating Federation..." );
		try
		{
			URL[] modules = new URL[]{
			    (new File("foms/CarsOnRoad.xml")).toURI().toURL(),
			};
			
			rtiAmb.createFederationExecution( "CarsOnRoad", modules );
			log( "Created Federation" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			log( "Didn't create federation, it already existed" );
		}
		catch( MalformedURLException urle )
		{
			log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
			urle.printStackTrace();
			return;
		}

		rtiAmb.joinFederationExecution( federateName,
		                                "Gui",
		                                "CarsOnRoad"
		                                 );

		log( "Joined Federation as " + federateName );

		this.timeFactory = (HLAfloat64TimeFactory) rtiAmb.getTimeFactory();

		rtiAmb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
		while( fedAmb.isAnnounced == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		waitForUser();

		rtiAmb.synchronizationPointAchieved( READY_TO_RUN );
		log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
		while( fedAmb.isReadyToRun == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		enableTimePolicy();
		log( "Time Policy Enabled" );

		publishAndSubscribe();
		log( "Published and Subscribed" );

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(GUIHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		isTimeGo = true;

		while( fedAmb.isRunning )
		{
			//aktualizacja statystyk
			if(isTimeGo){
				GUI.getInstance().updateTime();
			}

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", carsPositions - " + carsPosition );
			GUI.getInstance().panel.frame.carsPosition = carsPosition;
		}

		rtiAmb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		log( "Resigned from Federation" );

		try
		{
			rtiAmb.destroyFederationExecution( "ExampleFederation" );
			log( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			log( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			log( "Didn't destroy federation, federates still joined" );
		}
	}

	private void enableTimePolicy() throws Exception
	{
		HLAfloat64Interval lookahead = timeFactory.makeInterval( fedAmb.federateLookahead );

		this.rtiAmb.enableTimeRegulation( lookahead );
		while( fedAmb.isRegulating == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}

		this.rtiAmb.enableTimeConstrained();
		while( fedAmb.isConstrained == false )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}
	}

	private void publishAndSubscribe() throws RTIexception
	{
		//publikowanie atrybutow obiektu Gui
		this.GUIHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Gui" );
		this.GUIGuiHandle = rtiAmb.getAttributeHandle(GUIHandle, "gui" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(GUIGuiHandle);
		rtiAmb.publishObjectClassAttributes(GUIHandle, attributes);

		//subskrybowanie na atrybuty obiektu Cars
		attributes.clear();
		this.carHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Cars" );
		this.carPositionsHandle = rtiAmb.getAttributeHandle(carHandle, "positions" );
		attributes.add(carPositionsHandle);
		rtiAmb.subscribeObjectClassAttributes(carHandle, attributes);

		//subskrybowanie na atrybuty obiektu Road
		attributes.clear();
		this.roadHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Road" );
		this.roadPreparedHandle = rtiAmb.getAttributeHandle(roadHandle, "roadPrepared" );
		attributes.add(roadPreparedHandle);
		rtiAmb.subscribeObjectClassAttributes(roadHandle, attributes);

		//subskrybowanie na interakcje
		String iname = "HLAinteractionRoot.roadPrepareStart";
		roadPrepareStartHandle = rtiAmb.getInteractionClassHandle( iname );
		rtiAmb.subscribeInteractionClass(roadPrepareStartHandle);
	}

	private void advanceTime( double timestep ) throws RTIexception
	{
		fedAmb.isAdvancing = true;
		HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime + timestep );
		rtiAmb.timeAdvanceRequest( time );

		while( fedAmb.isAdvancing )
		{
			rtiAmb.evokeMultipleCallbacks( 0.1, 0.2 );
		}
	}

	private short getTimeAsShort()
	{
		return (short) fedAmb.federateTime;
	}
	private byte[] generateTag()
	{
		return ("(timestamp) "+System.currentTimeMillis()).getBytes();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static void main( String[] args )
	{
		String federateName = "Gui";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		try
		{
			new GUIFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			rtie.printStackTrace();
		}
	}
}