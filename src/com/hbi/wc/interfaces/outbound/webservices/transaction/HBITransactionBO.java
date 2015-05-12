package com.hbi.wc.interfaces.outbound.webservices.transaction;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import wt.enterprise.RevisionControlled;
import wt.fc.ObjectReference;
import wt.fc.WTObject;
import wt.org.WTUser;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;

import com.hbi.wc.interfaces.outbound.webservices.util.HBIChangeSetUtil;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIIntegrationClientUtil;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIProperties;
import com.lcs.wc.client.ClientContext;
import com.lcs.wc.flextype.FlexType;
import com.lcs.wc.flextype.FlexTypeCache;
import com.lcs.wc.foundation.LCSLifecycleManaged;
import com.lcs.wc.foundation.LCSLifecycleManagedHelper;
import com.lcs.wc.foundation.LCSLifecycleManagedLogic;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSPointsOfMeasure;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.LCSLog;

/**
 * HBITransactionBO.java
 * 
 * This class contains generic functions which are using to create Transaction Object(an event handler to sync data from source FlexPLM to target FlexPLM) for various FlexObjects
 * @author Abdul.Patel@Hanes.com
 * @since  March-27-2015
 */
public class HBITransactionBO 
{
	/**
	 * This function is using to initialize Transaction Object(Business Object) for the given data Map<String, Object>(which contains all necessary parameters of the Transaction BO)
	 * @param hbiTransactionObjDataMap - Map<String, Object>
	 * @return hbiTransactionsBussObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	private LCSLifecycleManaged createHBITransactionBusinessObject(Map<String, Object> hbiTransactionObjDataMap) throws WTException, WTPropertyVetoException
	{
		LCSLog.debug("### START HBITransactionBO.createHBITransactionBusinessObject(hbiTransactionObjDataMap) ###");
		FlexType hbiTransactionsTableFlexTypeObj = FlexTypeCache.getFlexTypeFromPath(HBIProperties.hbiTransactionBOFlexType);
		LCSLifecycleManaged hbiTransactionsBussObj = LCSLifecycleManaged.newLCSLifecycleManaged();
		hbiTransactionsBussObj.setFlexType(hbiTransactionsTableFlexTypeObj);
		hbiTransactionsBussObj.setValue(HBIProperties.hbiTransactionStatusKey, "pending");
		
		//Iterating the given Map(contains FlexTypeAttributeKey and FlexTypeAttributeValue) and updating the data on Transaction Object
		Object flexTypeAttributeValue = "";
		for(String flexTypeAttributeKey : hbiTransactionObjDataMap.keySet())
		{
			flexTypeAttributeValue = hbiTransactionObjDataMap.get(flexTypeAttributeKey);
			hbiTransactionsBussObj.setValue(flexTypeAttributeKey, flexTypeAttributeValue);
		}
		
		//Persisting the Transaction Object with all necessary details
		LCSLifecycleManagedLogic.deriveFlexTypeValues(hbiTransactionsBussObj);
		hbiTransactionsBussObj = LCSLifecycleManagedHelper.getService().saveLifecycleManaged(hbiTransactionsBussObj);
		
		LCSLog.debug("### END HBITransactionBO.createHBITransactionBusinessObject(hbiTransactionObjDataMap) ###");
		return hbiTransactionsBussObj;
	}
	
	/**
	 * This function is using to format/initialize and prepare the data Map<String, Object> for the given WTObject, which are using to initialize Transaction object for data sync
	 * @param wtObj - WTObject
	 * @param hbiTransactionObjDataMap - Map<String, Object>
	 * @return hbiTransactionsBussObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	private LCSLifecycleManaged createHBITransactionBusinessObject(WTObject wtObj, Map<String, Object> hbiTransactionObjDataMap) throws WTException, WTPropertyVetoException
	{
		LCSLog.debug("### START HBITransactionBO.createHBITransactionBusinessObject(wtObj, hbiTransactionObjDataMap) ###");
		Integer flexObjectId = (Integer) hbiTransactionObjDataMap.get(HBIProperties.hbiFlexObjectIdKey);
		Integer flexBranchId = (Integer) hbiTransactionObjDataMap.get(HBIProperties.hbiFlexBranchIdKey);
		String flexObjectOID = "";
		
		//Initialize FlexTypeAttribute data(Like FlexObjectName, FlexobjectOID, Event Triggered By) which are needed on Transaction object for data sync to the source FlexPLM
		String flexObjectClassName = wtObj.getClass().getName();
		String userID = (String) ClientContext.getContext().getUserId();
		WTUser eventTriggeredBy = (WTUser) LCSQuery.findObjectById(userID);
		
		//Validate the given WTObject(Like is an instance of RevisionControlled/LCSManaged) and format FlexObject BranchId/FlexObject ObjectId and FlexObject OID
		if(wtObj instanceof RevisionControlled)
		{
			flexObjectOID = "VR:".concat(flexObjectClassName).concat(":").concat(flexBranchId.toString());
		}
		else
		{
			flexObjectOID = "OR:".concat(flexObjectClassName).concat(":").concat(flexObjectId.toString());
		}
		
		//Adding dynamic parameters and values to the Map<String, Object>, which are using to create Transaction object for data sync from source FlexPLM to target FlexPLM
		hbiTransactionObjDataMap.put(HBIProperties.hbiEventTriggeredByKey, eventTriggeredBy);
		hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectClassNameKey, flexObjectClassName);
		hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectOIDKey, flexObjectOID);
		
		//Calling a function which will initialize the Transaction Object for the given Map<String, Object>(contains object/event specific data) in the pre-defined Path.
		LCSLifecycleManaged hbiTransactionsBussObj = createHBITransactionBusinessObject(hbiTransactionObjDataMap);
		
		LCSLog.debug("### END HBITransactionBO.createHBITransactionBusinessObject(wtObj, hbiTransactionObjDataMap) ###");
		return hbiTransactionsBussObj;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// TRANSACTION OBJECT INITIALIZATION:- following functions using in Transaction Object Initialization for various FlexObject /////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This function is invoking from generic plug-in functions, validating the given WTObject and invoking internal functions to initialize Transaction object for LCSMeasurements
	 * @param wtObj - WTObject
	 * @param eventType - String
	 * @return hbiTransactionsBussObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public LCSLifecycleManaged createHBIMeasurementsTransactionObject(LCSMeasurements measurementsObj, String eventType) throws WTException, WTPropertyVetoException, IllegalAccessException, InvocationTargetException
	{
		LCSLog.debug("### START HBITransactionBO.createHBIMeasurementsTransactionObject() ###");
		Map<String, Object> hbiTransactionObjDataMap = new HashMap<String, Object>();
		String flexObjectClassName = measurementsObj.getClass().getName();
		LCSLifecycleManaged hbiTransactionsBussObj = null;
		LCSMeasurements oldMeasurementsObj = null;
		boolean createdTransactionObject = true;
		String changeSet = "";

		//Calling a function which is using to get 'Transaction Object' Unique Identifier and 'Initialize  Transaction Object' status for the given LCSMeasurements instance
		String transactionObjUniqueIdentifier = getTransactionObjectUniqueIdentifierForMeasurementsObject(measurementsObj, eventType);
		boolean initializeTransactionObject = new HBIIntegrationClientUtil().getTransactionObjectInitializationStatusFor(transactionObjUniqueIdentifier, flexObjectClassName);
		
		//On Update of LCSMeasurements, validate the changeSet for the pre-defined list of attributes, based on changeSet status re-initialize Transaction object create status flag 
		if("update".equalsIgnoreCase(eventType) && initializeTransactionObject)
		{
			ObjectReference objRef = VersionControlHelper.getPredecessor((RevisionControlled) measurementsObj);
			if(objRef != null)
			{
				createdTransactionObject = false;
				oldMeasurementsObj = (LCSMeasurements) objRef.getObject();
				changeSet = new HBIChangeSetUtil().populateChangeSet(measurementsObj, oldMeasurementsObj, measurementsObj.getFlexType(), HBIProperties.measurementsProductScopeAttributes);
				if(FormatHelper.hasContent(changeSet))
					createdTransactionObject = true;
			}
		}
		
		//Based on the createdTransactionObject status, preparing data Map<String, Object> and invoking a function to initialize/create Transaction object for data sync
		if(createdTransactionObject && initializeTransactionObject)
		{
			//Calling a function which is using to update the given Map<String, Object> for FlexObject IDA2A2 or BranchIterationInfo for the given WTObject and Event Type
			hbiTransactionObjDataMap = updateTransactionObjectFlexObjectIdAndBranchId(measurementsObj, oldMeasurementsObj, hbiTransactionObjDataMap, eventType);
			
			//Adding dynamic parameters and values to the Map<String, Object> and calling a function which will internally invoke other functions to initialize the Transaction Object
			hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectNameKey, measurementsObj.getMeasurementsName());
			hbiTransactionObjDataMap.put(HBIProperties.hbiEventTypeKey, eventType);
			hbiTransactionObjDataMap.put(HBIProperties.hbiChangeSetKey, changeSet);
			hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectUniqueIdentifierKey, transactionObjUniqueIdentifier);
			hbiTransactionsBussObj = createHBITransactionBusinessObject(measurementsObj, hbiTransactionObjDataMap);
		}
		
		LCSLog.debug("### END HBITransactionBO.createHBIMeasurementsTransactionObject() ###");
		return hbiTransactionsBussObj;
	}
	
	/**
	 * This function is invoking from generic plug-in functions, validating the given WTObject and invoking internal functions to initialize Transaction object for PointsOfMeasure
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @param eventType - String
	 * @return hbiTransactionsBussObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public LCSLifecycleManaged createHBIPointsOfMeasureTransactionObject(LCSPointsOfMeasure pointsOfMeasureObj, String eventType) throws WTException, WTPropertyVetoException, IllegalAccessException, InvocationTargetException
	{
		LCSLog.debug("### START HBITransactionBO.createHBIPointsOfMeasureTransactionObject() ###");
		String flexObjectName = (String)pointsOfMeasureObj.getValue(HBIProperties.hbiGradeCodeKey);
		Map<String, Object> hbiTransactionObjDataMap = new HashMap<String, Object>();
		String flexObjectClassName = pointsOfMeasureObj.getClass().getName();
		HBIIntegrationClientUtil integrationClientUtilObj = new HBIIntegrationClientUtil();
		LCSLifecycleManaged hbiTransactionsBussObj = null;
		boolean createTransactionObject = true;
		String changeSet = "";
		
		//Calling a function which is using to get 'Transaction Object' Unique Identifier and 'Initialize  Transaction Object' status for the given LCSPointsOfMeasure instance
		String transactionObjUniqueIdentifier = getTransactionObjectUniqueIdentifierForMeasurementsObject(pointsOfMeasureObj, eventType);
		boolean initializeTransactionObject = integrationClientUtilObj.getTransactionObjectInitializationStatusFor(transactionObjUniqueIdentifier, flexObjectClassName);
		
		//On Update of LCSPointsOfMeasure, validate the changeSet for the pre-defined list of attributes, based on changeSet status re-initialize Transaction object create flag
		if("update".equalsIgnoreCase(eventType) && initializeTransactionObject)
		{	
			//Calling a function which will validate and return LCSPointsOfMeasure instance(previous version object) for the given LCSPointsOfMeasure instance and other parameters
			LCSPointsOfMeasure oldPointsOfMeasureObj = integrationClientUtilObj.getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj, false, false);
			
			if(!pointsOfMeasureObj.isDropped() && oldPointsOfMeasureObj != null)
			{
				createTransactionObject = false;
				changeSet = new HBIChangeSetUtil().populateChangeSet(pointsOfMeasureObj, oldPointsOfMeasureObj, pointsOfMeasureObj.getFlexType(), HBIProperties.measurementsMeasurementScopeAttributes);
				if(FormatHelper.hasContent(changeSet))
					createTransactionObject = true;
			}
		}
		
		//Based on the createdTransactionObject status, preparing data Map<String, Object> and invoking a function to initialize/create Transaction object for data sync
		if(createTransactionObject && initializeTransactionObject)
		{
			//Calling a function which is using to update the given Map<String, Object> for FlexObject IDA2A2 or BranchIterationInfo for the given WTObject and Event Type
			hbiTransactionObjDataMap = updateTransactionObjectFlexObjectIdAndBranchId(pointsOfMeasureObj, null, hbiTransactionObjDataMap, "create");
			
			//Adding dynamic parameters and values to the Map<String, Object> and calling a function which will internally invoke other functions to initialize the Transaction Object
			hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectNameKey, flexObjectName);
			hbiTransactionObjDataMap.put(HBIProperties.hbiEventTypeKey, eventType);
			hbiTransactionObjDataMap.put(HBIProperties.hbiChangeSetKey, changeSet);
			hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectUniqueIdentifierKey, transactionObjUniqueIdentifier);
			hbiTransactionsBussObj = createHBITransactionBusinessObject(pointsOfMeasureObj, hbiTransactionObjDataMap);
		}
		
		LCSLog.debug("### END HBITransactionBO.createHBIPointsOfMeasureTransactionObject() ###");
		return hbiTransactionsBussObj;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// PRIVATE FUNCTIONS:- All the functions are related to fetching details using in Transaction Object Initialization //////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * This function is using to get Transaction Object Unique Identifier for the given LCSPointsOfMeasure, which is using on determine the creation of Event Transaction Object
	 * @param wtObj - WTObject
	 * @return transactionObjUniqueIdentifier - String
	 * @return eventType - String
	 * @throws WTException
	 */
	private String getTransactionObjectUniqueIdentifierForMeasurementsObject(WTObject wtObj, String eventType) throws WTException
	{
		// LCSLog.debug("### START HBITransactionBO.getTransactionObjectUniqueIdentifierForMeasurementsObject(wtObj) ###");
		String transactionObjUniqueIdentifier = "";
		String measurementsMasterIdentifier = "";
		
		//Initializing Measurements Master Identifier and LCSPointsOfMeasure Branch Identifier, which are using to get the existing Transaction object in pending state
		if(wtObj instanceof LCSPointsOfMeasure)
		{
			LCSPointsOfMeasure pointsOfMeasureObj = (LCSPointsOfMeasure) wtObj;
			if(pointsOfMeasureObj.getMeasurementsMaster() != null)
			{
				measurementsMasterIdentifier = FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj.getMeasurementsMaster());
			}
			else
			{
				measurementsMasterIdentifier = FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj);
			}
			
			Integer branchId = pointsOfMeasureObj.getId();
			transactionObjUniqueIdentifier = measurementsMasterIdentifier.toString().concat(":").concat(branchId.toString());
		}
		//Initializing Measurements Branch Identifier, which are using to get the existing Transaction object in pending state/update unique identifier on each transaction object
		else if(wtObj instanceof LCSMeasurements)
		{
			LCSMeasurements measurementsObj = (LCSMeasurements) wtObj;
			if("update".equalsIgnoreCase(eventType))
			{
				ObjectReference objRef = VersionControlHelper.getPredecessor((RevisionControlled) measurementsObj);
				measurementsObj = (LCSMeasurements) objRef.getObject();
			}
			
			if(measurementsObj != null)
				transactionObjUniqueIdentifier = FormatHelper.getNumericVersionIdFromObject(measurementsObj);
		}
		
		// LCSLog.debug("### END HBITransactionBO.getTransactionObjectUniqueIdentifierForMeasurementsObject(wtObj) ###");
		return transactionObjUniqueIdentifier;
	}
	
	/**
	 * This function is using to update the given Map<String, Object> for FlexObject IDA2A2 or BranchIterationInfo for the given WTObject and Event Type(create event / update event)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param hbiTransactionObjDataMap - Map<String, Object>
	 * @param eventType - String
	 * @return hbiTransactionObjDataMap - Map<String, Object>
	 * @throws WTException
	 */
	private Map<String, Object> updateTransactionObjectFlexObjectIdAndBranchId(WTObject currentObj, WTObject oldObj, Map<String, Object> hbiTransactionObjDataMap, String eventType) throws WTException
	{
		// LCSLog.debug("### START HBITransactionBO.updateTransactionObjectFlexObjectIdAndBranchId(currentObj, oldObj, hbiTransactionObjDataMap) ###");
		Integer flexObjectId = 0; 
		Integer flexBranchId = 0;
		
		//CREATE EVENT: Validate the given WTObject(Like is an instance of RevisionControlled/LCSManaged) and format FlexObject BranchId/FlexObject ObjectId to append the given Map
		if("create".equalsIgnoreCase(eventType))
		{
			if(currentObj instanceof RevisionControlled)
			{
				flexBranchId = Integer.parseInt(FormatHelper.getNumericVersionIdFromObject((RevisionControlled)currentObj));
			}
			else
			{
				flexObjectId = Integer.parseInt(FormatHelper.getNumericObjectIdFromObject(currentObj));
			}
		}
		//UPDATE EVENT: Validate the given WTObject(Like is an instance of RevisionControlled/LCSManaged) and format FlexObject BranchId/FlexObject ObjectId to append the given Map
		else if("update".equalsIgnoreCase(eventType))
		{
			if(currentObj instanceof RevisionControlled)
			{
				flexBranchId = Integer.parseInt(FormatHelper.getNumericVersionIdFromObject((RevisionControlled)oldObj));
			}
			else
			{
				flexObjectId = Integer.parseInt(FormatHelper.getNumericObjectIdFromObject(oldObj));
			}
		}
		
		//Adding dynamic parameters and values to the Map<String, Object> (adding FlexObjectId and FlexBranchId) and returning Map<String, Object> from the function header
		hbiTransactionObjDataMap.put(HBIProperties.hbiFlexBranchIdKey, flexBranchId);
		hbiTransactionObjDataMap.put(HBIProperties.hbiFlexObjectIdKey, flexObjectId);
		
		// LCSLog.debug("### END HBITransactionBO.updateTransactionObjectFlexObjectIdAndBranchId(currentObj, oldObj, hbiTransactionObjDataMap) ###");
		return hbiTransactionObjDataMap;
	}
}