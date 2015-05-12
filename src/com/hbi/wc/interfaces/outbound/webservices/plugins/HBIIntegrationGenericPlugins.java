package com.hbi.wc.interfaces.outbound.webservices.plugins;

import java.lang.reflect.InvocationTargetException;

import com.hbi.wc.interfaces.outbound.webservices.transaction.HBITransactionBO;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIIntegrationClientUtil;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSPointsOfMeasure;
import com.lcs.wc.util.LCSLog;

import wt.fc.WTObject;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * HBIIntegrationGenericPlugins.java
 * 
 * This class contains generic functions which will invoke on create, update & delete event of FlexObject(which are sub set of WTObject) to create Transaction BO for data processing 
 * @author Abdul.Patel@Hanes.com
 * @since  March-26-2015
 */
public class HBIIntegrationGenericPlugins 
{
	/**
	 * Plug-in function which will invoke on post create persist event of WTObject, using for data integration from source FlexPLM to target FlexPLM for the pre-defined FlexObjects
	 * @param wtObj - WTObject
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void redirectCreateEventTransactionListener(WTObject wtObj) throws WTException, WTPropertyVetoException, IllegalAccessException, InvocationTargetException
	{
		LCSLog.debug("### START HBIIntegrationGenericPlugins.redirectCreateEventTransactionListener() ###");
		
		if(wtObj instanceof LCSMeasurements)
		{
			//Calling a function to get 'Transaction BO' Initialization status(in case of Instance data, we need to consider only 'Block Products') and invoke function to create BO
			boolean initializeTransactionObject = new HBIIntegrationClientUtil().getTransactionObjectInitializationStatusFor(wtObj, "create");
			if(initializeTransactionObject)
			{
				//Calling a function which is using to validate and initialize Transaction object for the given WTObject(LCSMeasurements) for data sync from source FlexPLM to target PLM
				new HBITransactionBO().createHBIMeasurementsTransactionObject((LCSMeasurements)wtObj, "create");
			}
		}
		else if(wtObj instanceof LCSPointsOfMeasure)
		{
			LCSPointsOfMeasure pointsOfMeasureObj = (LCSPointsOfMeasure)wtObj;
			
			//Calling a function to get 'Transaction BO' Initialization status(in case of Instance data, we need to consider only 'Block Products') and invoke function to create BO
			boolean initializeTransactionObject = new HBIIntegrationClientUtil().getTransactionObjectInitializationStatusFor(wtObj, "create");
			if(initializeTransactionObject)
			{
				//CREATE EVENT: Validate the given object effectSequence(using to derive the actual event) and calling a function to initialize Transaction object for data integration
				if(pointsOfMeasureObj.getEffectSequence() == 0)
				{
					new HBITransactionBO().createHBIPointsOfMeasureTransactionObject(pointsOfMeasureObj, "create");
				}
				//UPDATE EVENT: Validate the given object effectSequence(using to derive the actual event) and calling a function to initialize Transaction object for data integration
				else
				{
					new HBITransactionBO().createHBIPointsOfMeasureTransactionObject(pointsOfMeasureObj, "update");
				}
			}
		}
		
		LCSLog.debug("### END HBIIntegrationGenericPlugins.redirectCreateEventTransactionListener() ###");
	}
	
	/**
	 * Plug-in function which will invoke on pre update persist event of WTObject, using for data integration from source FlexPLM to target FlexPLM for the pre-defined FlexObjects
	 * @param wtObj - WTObject
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void redirectUpdateEventTransactionListener(WTObject wtObj) throws WTException, WTPropertyVetoException, IllegalAccessException, InvocationTargetException
	{
		LCSLog.debug("### START HBIIntegrationGenericPlugins.redirectUpdateEventTransactionListener() ###");
		
		if(wtObj instanceof LCSMeasurements)
		{
			//Calling a function to get 'Transaction BO' Initialization status(in case of Instance data, we need to consider only 'Block Products') and invoke function to create BO
			boolean initializeTransactionObject = new HBIIntegrationClientUtil().getTransactionObjectInitializationStatusFor(wtObj, "update");
			if(initializeTransactionObject)
			{
				//Calling a function which is using to validate and initialize Transaction object for the given WTObject(LCSMeasurements) for data sync from source FlexPLM to target PLM
				new HBITransactionBO().createHBIMeasurementsTransactionObject((LCSMeasurements)wtObj, "update");
			}
		}
		else if(wtObj instanceof LCSPointsOfMeasure)
		{
			LCSPointsOfMeasure pointsOfMeasureObj = (LCSPointsOfMeasure)wtObj;
			
			//Calling a function to get 'Transaction BO' Initialization status(in case of Instance data, we need to consider only 'Block Products') and invoke function to create BO
			boolean initializeTransactionObject = new HBIIntegrationClientUtil().getTransactionObjectInitializationStatusFor(wtObj, "update");
			if(initializeTransactionObject)
			{
				//UPDATE EVENT: Validate the given object effectSequence(using to derive the actual event) and calling a function to initialize Transaction object for data integration
				if(pointsOfMeasureObj.getEffectOutDate() == null || pointsOfMeasureObj.isDropped())
				{
					new HBITransactionBO().createHBIPointsOfMeasureTransactionObject(pointsOfMeasureObj, "update");
				}
			}
		}
		
		LCSLog.debug("### END HBIIntegrationGenericPlugins.redirectUpdateEventTransactionListener() ###");
	}
	
	/**
	 * This function will invoke on delete of FlexObject(which are subset of WTObject) from source FlexPLM, which are using to process delete request to the Integrated PLM system
	 * @param wtObj - WTObject
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void redirectDeleteEventTransactionListener(WTObject wtObj) throws WTException, WTPropertyVetoException
	{
		LCSLog.debug("### START HBIIntegrationGenericPlugins.redirectDeleteEventTransactionListener() ###");
		
		if(wtObj instanceof LCSMeasurements || wtObj instanceof LCSPointsOfMeasure)
		{
			//Calling a function, which is using to delete the FlexObject(of type LCSMeasurements/LCSPintsOfMeasure) from target FlexPLM based on the given parameter from source PLM
			
		}
		
		LCSLog.debug("### END HBIIntegrationGenericPlugins.redirectDeleteEventTransactionListener() ###");
	}
}