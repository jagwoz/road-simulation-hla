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
package Cars;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class CarFederate
{
	public static final String READY_TO_RUN = "ReadyToRun";
	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiAmb;
	private CarFederateAmbassador fedAmb;
	private HLAfloat64TimeFactory timeFactory;
	protected EncoderFactory encoderFactory;

	//road federate
	protected ObjectClassHandle roadHandle;
	protected AttributeHandle roadPreparedHandle;
	protected InteractionClassHandle roadPrepareStartHandle;

	//car federate
	protected ObjectClassHandle carHandle;
	protected AttributeHandle carPositionHandle;

	protected int numberOfCars;
	protected ArrayList<Car> cars;
	protected ArrayList<Integer> carsPositions;
	protected boolean isRoadPrepared;
	protected Random random;

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void log( String message )
	{
		System.out.println( "CarFederate:    " + message );
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
		log( "Creating RTIambassador" );
		rtiAmb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

		log( "Connecting..." );
		fedAmb = new CarFederateAmbassador( this );
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
		                                "car",
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

		ObjectInstanceHandle objectHandle = rtiAmb.registerObjectInstance(carHandle);
		log( "Registered Terrain, handle=" + objectHandle );

		cars = new ArrayList<>();
		carsPositions = new ArrayList<>();
		isRoadPrepared = true;
		random = new Random();
		numberOfCars = random.nextInt(10) + 1;

		for(int i = 0; i < numberOfCars; i++){
			Car car = new Car(cars);
			cars.add(car);
			carsPositions.add(car.getPosition());
		}

		while( fedAmb.isRunning )
		{
			//wysylanie do RTI aktualnych pozycji samochodow
			AttributeHandleValueMap attributes = rtiAmb.getAttributeHandleValueMapFactory().create(1);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(carsPositions);
			attributes.put(carPositionHandle, bos.toByteArray());
			HLAfloat64Time time = timeFactory.makeTime( fedAmb.federateTime + fedAmb.federateLookahead );

			rtiAmb.updateAttributeValues( objectHandle, attributes, generateTag(), time );

			//zmiana pozycji samochodow jezeli droga jest w stanie gotowosci
			if (isRoadPrepared)
				for (Car car : cars){
					car.updatePosition(cars, cars.indexOf(car));
					carsPositions.set(cars.indexOf(car), car.getPosition());
				}

			//wysylanie interakcji jezeli ostatni pojazd zakonczyl trase
			if(!cars.isEmpty())
				if(cars.get(cars.size() - 1).isEndOfRide() && isRoadPrepared) {
					ParameterHandleValueMap parameterHandleValueMap = rtiAmb.getParameterHandleValueMapFactory().create(0);
					rtiAmb.sendInteraction(roadPrepareStartHandle, parameterHandleValueMap, generateTag(), time);
					log("Interaction newCarsCreate send.");
					//int lastPos = Car.getInstance().getStartPos();

					cars.clear();
					carsPositions.clear();

					numberOfCars = random.nextInt(10) + 1;
					for(int i = 0; i < numberOfCars; i++){
						Car car = new Car(cars);
						cars.add(car);
						carsPositions.add(car.getPosition());
					}

					isRoadPrepared = false;
				}

			advanceTime(1);
			log( "Time Advanced to " + fedAmb.federateTime +
					", positions - " + carsPositions);
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
		//publikowanie atrybutow przez obiekt Cars
		this.carHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Cars" );
		this.carPositionHandle = rtiAmb.getAttributeHandle(carHandle, "positions" );
		AttributeHandleSet attributes = rtiAmb.getAttributeHandleSetFactory().create();
		attributes.add(carPositionHandle);
		rtiAmb.publishObjectClassAttributes(carHandle, attributes);

		//subskrybowanie na atrybuty obiektu Road
		attributes.clear();
		this.roadHandle = rtiAmb.getObjectClassHandle( "HLAobjectRoot.Road" );
		this.roadPreparedHandle = rtiAmb.getAttributeHandle(roadHandle, "roadPrepared" );
		attributes.add(roadPreparedHandle);
		rtiAmb.subscribeObjectClassAttributes(roadHandle, attributes);

		//publikowanie interakcji
		String iName = "HLAinteractionRoot.roadPrepareStart";
		roadPrepareStartHandle = rtiAmb.getInteractionClassHandle( iName );
		rtiAmb.publishInteractionClass(roadPrepareStartHandle);
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
		String federateName = "Cars";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		try
		{
			new CarFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			rtie.printStackTrace();
		}
	}
}