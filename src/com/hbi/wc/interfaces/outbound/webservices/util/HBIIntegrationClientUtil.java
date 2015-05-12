package com.hbi.wc.interfaces.outbound.webservices.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import wt.fc.WTObject;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

import com.lcs.wc.db.Criteria;
import com.lcs.wc.db.FlexObject;
import com.lcs.wc.db.PreparedQueryStatement;
import com.lcs.wc.db.QueryColumn;
import com.lcs.wc.db.SearchResults;
import com.lcs.wc.flextype.FlexType;
import com.lcs.wc.flextype.FlexTypeCache;
import com.lcs.wc.foundation.LCSLifecycleManaged;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSMeasurementsMaster;
import com.lcs.wc.measurements.LCSPointsOfMeasure;
import com.lcs.wc.season.LCSSeason;
import com.lcs.wc.specification.FlexSpecQuery;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.VersionHelper;

/**
 * HBIIntegrationClientUtil.java
 * 
 * This class contains generic functions which are using to get List<String> of FlexTypeAttributes, getValue and setValue function for the given WTObject and other generic functions
 * @author Abdul.Patel@Hanes.com
 * @since  APRIL-1-2015
 */
public class HBIIntegrationClientUtil
{
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////// TRANSACTION OBJECT FUNCTIONS:- Following all the functions are using in Transaction Object Validation and Initialization for various FlexObjects //////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This function is using to format and return List of FlextypeAttributeKeys for the given PropertyEntryValue(which contains set of FlexTypeAttribute Keys with comma separator)
	 * @param propertyEntryValue - String
	 * @return flexTypeAttributeKeysList - List<String>
	 */
	public List<String> getFlexTypeAttributeKeysFromPropertiesFile(String propertyEntryValue)
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getFlexTypeAttributeKeysFromPropertiesFile(propertyEntryValue) ###");
		List<String> flexTypeAttributeKeysList = new ArrayList<String>();

		//Validating the given Property Entry Value(which contains set of FlexTypeAttribute Keys with comma as delimiter) and forming StringTokenizer instance
		if (FormatHelper.hasContent(propertyEntryValue))
		{
			StringTokenizer strTokenFlexTypeAttributeKeys = new StringTokenizer(propertyEntryValue, ",");

			//Iterating the StringTokenizer instance, get FlexTypeAttribute-Key, adding FlexTypeAttributeKey to the contains to return from the function header
			while (strTokenFlexTypeAttributeKeys.hasMoreTokens())
			{
				String flexTypeAttributeKey = strTokenFlexTypeAttributeKeys.nextToken().trim();
				flexTypeAttributeKeysList.add(flexTypeAttributeKey);
			}
		}

		// LCSLog.debug("### END HBIIntegrationClientUtil.getFlexTypeAttributeKeysFromPropertiesFile(propertyEntryValue) ###");
		return flexTypeAttributeKeysList;
	}
	
	/**
	 * This function is using to return Method(Reflection API) instance (which contains getValue function) from the function header for the given FlexObject (instance of WTObject) 
	 * @param wtobj - WTObject
	 * @return method - Method
	 */
	public Method getFlexObjectgetValueMethod(WTObject wtobj)
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getFlexObjectgetValueMethod() ###");
		
		Method method = null;
		try
		{
			method = wtobj.getClass().getMethod("getValue", new Class[] { String.class });
		}
		catch (NoSuchMethodException methodexp)
		{
			methodexp.printStackTrace();
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getFlexObjectgetValueMethod() ###");
		return method;
	}
	
	/**
	 * This function is using to return Method(Reflection API) instance (which contains setValue function) from the function header for the given FlexObject (instance of WTObject) 
	 * @param wtobj - WTObject
	 * @return method - Method
	 */
	public Method getFlexObjectsetValueMethod(WTObject wtobj)
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getFlexObjectsetValueMethod() ###");
		
		Method method = null;
		try
		{
			method = wtobj.getClass().getMethod("setValue", new Class[] { String.class });
		}
		catch (NoSuchMethodException methodexp)
		{
			methodexp.printStackTrace();
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getFlexObjectsetValueMethod() ###");
		return method;
	}
	
	/**
	 * This function is using to get create event version/latest version/previous version of LCSPointsOfMeasure for the given LCSPointsOfMeasure instance and other input parameters
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @param createEvent - boolean
	 * @param latestVersion - boolean
	 * @return pointsOfMeasureObj - LCSPointsOfMeasure
	 * @throws WTException
	 */
	public LCSPointsOfMeasure getLatestOrOldPointsOfMeasureFor(LCSPointsOfMeasure pointsOfMeasureObj, boolean createEvent, boolean latestVersion) throws WTException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj) ###");
		Integer effectSequence = pointsOfMeasureObj.getEffectSequence();
		Long measurementsMasterIdentifier = 0L;
		String objectIdentifierColumnName = "";
		
		//CREATE EVENT LINK: Initializing EffectSequence to get Create Event Link(LCSPointsOfMeasure instance) for the given LCSPointsOfMeasure object to return from function 
		if(createEvent)
		{
			effectSequence = 0;
		}
		//LATEST EVENT LINK: Initializing EffectSequence to get Latest Event Link(LCSPointsOfMeasure instance) for the given LCSPointsOfMeasure object to return from function
		else if(latestVersion)
		{
			effectSequence = effectSequence + 1;
		}
		//PREVIOUS EVENT LINK: Initializing EffectSequence to get Previous Event Link(LCSPointsOfMeasure instance) for the given LCSPointsOfMeasure object to return from function
		else
		{
			effectSequence = effectSequence - 1;
		}
		
		//Initializing Measurements Master/LCSPointsOfMeasure Identifier and LCSPointsOfMeasure Branch Identifier, which are using to get the requested LCSPointsOfMeasure instance
		if(pointsOfMeasureObj.getMeasurementsMaster() != null)
		{
			measurementsMasterIdentifier = Long.parseLong(FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj.getMeasurementsMaster()));
			objectIdentifierColumnName = "measurementsMasterReference.key.id";
		}
		else
		{
			measurementsMasterIdentifier = Long.parseLong(FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj));
			objectIdentifierColumnName = "thePersistInfo.theObjectIdentifier.id";
		}
		
		//Calling a function which is using to get LCSPointsOfMeasure instance for the given set of parameters(Measurements Master Identifier, Branch Id and Effect Sequence)
		Integer branchId = pointsOfMeasureObj.getId();
		pointsOfMeasureObj = getLatestOrOldPointsOfMeasureFor(objectIdentifierColumnName, measurementsMasterIdentifier, branchId, effectSequence);
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj) ###");
		return pointsOfMeasureObj;
	}
	
	/**
	 * This function is using to get LCSPointsOfMeasure instance for the given set of input parameters(Measurements Master Identifier, Branch Id and Effect Sequence of given object)
	 * @param objectIdentifierColumnName - String
	 * @param measurementsMasterIdentifier - Long
	 * @param branchId - Integer
	 * @param effectSequence - Integer
	 * @return pointsOfMeasureObj - LCSPointsOfMeasure
	 * @throws WTException
	 */
	private LCSPointsOfMeasure getLatestOrOldPointsOfMeasureFor(String objectIdentifierColumnName, Long measurementsMasterIdentifier, Integer branchId, Integer effectSequence) throws WTException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj, effectSequence) ###");
		LCSPointsOfMeasure pointsOfMeasureObj = null;
		
		//Initializing the PreparedQueryStatement, which is using to get LCSPointsOfMeasure object based on the given set of parameters(unique object identifier)
		PreparedQueryStatement statement = new PreparedQueryStatement();
		statement.appendSelectColumn(new QueryColumn(LCSPointsOfMeasure.class, "thePersistInfo.theObjectIdentifier.id"));
        statement.appendFromTable(LCSPointsOfMeasure.class);
        statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, "id"), "?", "="), branchId);
        statement.appendAndIfNeeded();
        statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, "effectSequence"), "?", "="), effectSequence);
		
        //Validating the given additional parameter (object unique identifier) and appending the criteria's to get specific version  of LCSPointsOfMeasure object from the given info
        if(FormatHelper.hasContent(objectIdentifierColumnName))
        {
        	statement.appendAndIfNeeded();
        	statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, objectIdentifierColumnName), "?", "="), measurementsMasterIdentifier);
        }
        
        //Get FlexObject from the SearchResults instance, which is using to form LCSPointsOfMeasure instance, which is needed to return from the function header
        SearchResults results = LCSQuery.runDirectQuery(statement);
        if(results != null && results.getResultsFound() > 0)
        {
        	FlexObject flexObj = (FlexObject)results.getResults().get(0);
        	pointsOfMeasureObj = (LCSPointsOfMeasure)LCSQuery.findObjectById("OR:com.lcs.wc.measurements.LCSPointsOfMeasure:"+ flexObj.getString("LCSPointsOfMeasure.IDA2A2"));
        }
        
		// LCSLog.debug("### END HBIIntegrationClientUtil.getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj, effectSequence) ###");
		return pointsOfMeasureObj;
	}
	
	/**
	 * This function is using to validate the given PointsOfMeasure Type(Library/ Template, Instance, Instance_Specific, gradings) & get Latest LCSPointsOfMeasure for the given object
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @return pointsOfMeasureObj - LCSPointsOfMeasure
	 * @throws WTException
	 */
	public LCSPointsOfMeasure getLatestPointsOfMeasureFor(LCSPointsOfMeasure pointsOfMeasureObj) throws WTException
	{
		// LCSLog.debug("### END HBIIntegrationClientUtil.getLatestPointsOfMeasureFor(pointsOfMeasureObj) ###");
		Integer branchId = pointsOfMeasureObj.getId();
		
		//Validate the PointsOfMeasure Type(Library/ Template, Instance, Instance_Specific), get Latest LCSPointsOfMeasure for the given LCSPointsOfMeasure and return from header
		if(pointsOfMeasureObj.getMeasurementsMaster() != null)
		{
			long measurementsMasterIdentifier = Long.parseLong(FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj.getMeasurementsMaster()));
			
			//Initializing the PreparedQueryStatement, which is using to get LCSPointsOfMeasure object based on the given set of parameters(unique object identifier, id and outDate)
			PreparedQueryStatement statement = new PreparedQueryStatement();
			statement.appendSelectColumn(new QueryColumn(LCSPointsOfMeasure.class, "thePersistInfo.theObjectIdentifier.id"));
	        statement.appendFromTable(LCSPointsOfMeasure.class);
	        statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, "measurementsMasterReference.key.id"), "?", "="), measurementsMasterIdentifier);
	        statement.appendAndIfNeeded();
	        statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, "id"), "?", "="), branchId);
	        statement.appendAndIfNeeded();
	        statement.appendCriteria(new Criteria(new QueryColumn(LCSPointsOfMeasure.class, "effectOutDate"), "", Criteria.IS_NULL));
	        
	        //Get FlexObject from the SearchResults instance, which is using to form LCSPointsOfMeasure instance latest object, which is needed to return from the function header
	        SearchResults results = LCSQuery.runDirectQuery(statement);
	        if(results != null && results.getResultsFound() > 0)
	        {
	        	FlexObject flexObj = (FlexObject)results.getResults().get(0);
	        	pointsOfMeasureObj = (LCSPointsOfMeasure)LCSQuery.findObjectById("OR:com.lcs.wc.measurements.LCSPointsOfMeasure:"+ flexObj.getString("LCSPointsOfMeasure.IDA2A2"));
	        }
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getLatestPointsOfMeasureFor(pointsOfMeasureObj) ###");
		return pointsOfMeasureObj;
	}
	
	/**
	 * This function is using to validate the existence of transaction object for the given unique identifier and class name, return the status(true/false) from the function header 
	 * @param transactionObjUniqueIdentifier - String
	 * @param flexObjectClassName - String
	 * @return true/false - boolean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public boolean getTransactionObjectInitializationStatusFor(String transactionObjUniqueIdentifier, String flexObjectClassName) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getTransactionObjectInitializationStatusFor(transactionObjUniqueIdentifier, flexObjectClassName) ###");
		FlexType transactionObjectFlexType = FlexTypeCache.getFlexTypeFromPath(HBIProperties.hbiTransactionBOFlexType);
		String id = String.valueOf(transactionObjectFlexType.getPersistInfo().getObjectIdentifier().getId());
		String uniqueIdentifierDBColumn = transactionObjectFlexType.getAttribute(HBIProperties.hbiFlexObjectUniqueIdentifierKey).getVariableName();
		String flexObjectClassNameDBColumn = transactionObjectFlexType.getAttribute(HBIProperties.hbiFlexObjectClassNameKey).getVariableName();
		String transactionStatusDBColumn = transactionObjectFlexType.getAttribute(HBIProperties.hbiTransactionStatusKey).getVariableName();
		
		//Initializing the PreparedQueryStatement, which is using to get LCSLifecycleManaged object based on the given set of parameters(unique object identifier, object class name)
		PreparedQueryStatement statement = new PreparedQueryStatement();
		statement.appendSelectColumn(new QueryColumn(LCSLifecycleManaged.class, "thePersistInfo.theObjectIdentifier.id"));
        statement.appendFromTable(LCSLifecycleManaged.class);
        statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, transactionStatusDBColumn), "pending", Criteria.EQUALS));
        statement.appendAndIfNeeded();
        statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, uniqueIdentifierDBColumn), transactionObjUniqueIdentifier, Criteria.EQUALS));
        statement.appendAndIfNeeded();
        statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, flexObjectClassNameDBColumn), flexObjectClassName, Criteria.EQUALS));
        statement.appendAndIfNeeded();
        statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, "flexTypeReference.key.id"), "?", "="), new Long(id));
        
        //Get SearchResults instance from the given statement object, get number of FlexObjects from SearchResults instances which are using to initialize object creation status
        SearchResults results = LCSQuery.runDirectQuery(statement);
        if(results != null && results.getResultsFound() > 0)
        {
        	return false;
        }
        
		// LCSLog.debug("### END HBIIntegrationClientUtil.getTransactionObjectInitializationStatusFor(transactionObjUniqueIdentifier, flexObjectClassName) ###");
        return true;
	}
	
	/**
	 * This function is using to get transaction object initialization status by validating the given measurement type and measurement product type (Block Products) & return status
	 * @param wtObj - WTObject
	 * @param eventType - String
	 * @return initializeTransactionObject - boolean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	@SuppressWarnings("unchecked")
	public boolean getTransactionObjectInitializationStatusFor(WTObject wtObj, String eventType) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getTransactionObjectInitializationStatusFor(wtObj, eventType) ###");
		boolean initializeTransactionObject = true;
		LCSMeasurements measurementsObj = null;
		
		//Validate the incoming WTObject (is an instance of LCSMeasurements/LCSPointsOfMeasure) and down cast to the specific object, which is using to validate HBI business rule
		if(wtObj instanceof LCSMeasurements)
		{
			measurementsObj = (LCSMeasurements) wtObj;
		}
		else if(wtObj instanceof LCSPointsOfMeasure)
		{
			LCSPointsOfMeasure pointsOfMeasureObj = (LCSPointsOfMeasure) wtObj;
			measurementsObj = pointsOfMeasureObj.getMeasurements();
			if(pointsOfMeasureObj.getEffectSequence() > 0)
				eventType = "update";
		}
		
		//Get LCSMeasurements Type(if it is an INSTANCE type) & event type is 'UPDATE' then validate the Product Type(Block Products) of the Measurement & Initialize Transaction BO
		if(measurementsObj != null)
		{
			String measurementType = measurementsObj.getMeasurementsType();
			if("INSTANCE".equalsIgnoreCase(measurementType) && "update".equalsIgnoreCase(eventType))
			{
				initializeTransactionObject = false;
				Collection<FlexObject> measurementComponentsCollection = FlexSpecQuery.componentWhereUsed((LCSMeasurementsMaster)measurementsObj.getMaster());
				if(measurementComponentsCollection != null && measurementComponentsCollection.size() > 0 )
				{
					FlexObject flexObj = measurementComponentsCollection.iterator().next();
					LCSSeason seasonObj = (LCSSeason) LCSQuery.findObjectById("VR:com.lcs.wc.season.LCSSeason:"+flexObj.getString("SPECTOLATESTITERSEASON.SEASONBRANCHID")); 
					seasonObj = (LCSSeason) VersionHelper.latestIterationOf(seasonObj);
					
					//Initialize the Transaction Object for the given Measurement Instance only if the given Measurement is associated with Block Product Type as per HBI business rule 
					if(HBIProperties.hbiBlockProductsSeasonFlexType.equalsIgnoreCase(seasonObj.getFlexType().getFullName(true)))
					{
						initializeTransactionObject = true;
					}
				}
			}
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getTransactionObjectInitializationStatusFor(wtObj, eventType) ###");
		return initializeTransactionObject;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// TRANSACTION OBJECT FUNCTIONS:- Following functions are using to fetch Transaction Object in pending state ///////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This function is using to validate and return Collection<FlexObject> (contains Business Object from Integration\Outbound\Transaction BO) for the given FlexObject class name
	 * @param flexObjectClassName - String
	 * @return hbiTransactionObjectCollection - Collection<FlexObject>
	 * @throws WTException
	 */
	@SuppressWarnings("unchecked")
	public Collection<FlexObject> getHBITransactionBusinessObjectForPendingStatus(String flexObjectClassName) throws WTException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.getHBITransactionBusinessObjectForPendingStatus(flexObjectClassName) ###");
		FlexType transactionObjectFlexType = FlexTypeCache.getFlexTypeFromPath(HBIProperties.hbiTransactionBOFlexType);
		String typeIdPath = String.valueOf(transactionObjectFlexType.getPersistInfo().getObjectIdentifier().getId());
		String flexObjectClassNameDBColumn = transactionObjectFlexType.getAttribute(HBIProperties.hbiFlexObjectClassNameKey).getVariableName();
		String transactionStatusDBColumn = transactionObjectFlexType.getAttribute(HBIProperties.hbiTransactionStatusKey).getVariableName();
		Collection<FlexObject> hbiTransactionObjectCollection = new ArrayList<FlexObject>();
		//int maxQueryLimit = Integer.parseInt(HBIProperties.hbiTransactionBOFetchLimit);
		
		//Initializing the PreparedQueryStatement, which is using to get LCSLifecycleManaged object based on the given set of parameters(FlexObject class name. from and to index)
		PreparedQueryStatement statement = new PreparedQueryStatement();
		statement.appendSelectColumn(new QueryColumn(LCSLifecycleManaged.class, "thePersistInfo.theObjectIdentifier.id"));
		statement.appendFromTable(LCSLifecycleManaged.class);
		statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, transactionStatusDBColumn), "pending", Criteria.EQUALS));
		statement.appendAndIfNeeded();
		statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, flexObjectClassNameDBColumn), flexObjectClassName, Criteria.EQUALS));
		statement.appendAndIfNeeded();
		statement.appendCriteria(new Criteria(new QueryColumn(LCSLifecycleManaged.class, "flexTypeReference.key.id"), "?", "="), new Long(typeIdPath));
		statement.appendSortBy(new QueryColumn(LCSLifecycleManaged.class, "thePersistInfo.createStamp"), "ASC");
		statement.appendSortBy(new QueryColumn(LCSLifecycleManaged.class, "thePersistInfo.theObjectIdentifier.id"), "ASC");
		//statement.setFromIndex(1);
		//statement.setToIndex(maxQueryLimit);
		statement.setDistinct(true);
		
		// LCSLog.debug(" Query to fetch Pending Transaction BO : "+ statement);
		SearchResults results = LCSQuery.runDirectQuery(statement);
		if(results != null && results.getResultsFound() > 0)
		{
			hbiTransactionObjectCollection = results.getResults();
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.getHBITransactionBusinessObjectForPendingStatus(flexObjectClassName) ###");
		return hbiTransactionObjectCollection;
	}
	
	/**
	 * This function is using to get the transaction object status(is valid transaction object to invoke data sync process) for the given transaction object & measurement instance
	 * @param transactionObj - LCSLifecycleManaged
	 * @param measurementsObj - LCSMeasurements
	 * @return transactionObjectStatus - boolean
	 * @throws WTException
	 */
	@SuppressWarnings("unchecked")
	public boolean isValidTransactionObject(LCSLifecycleManaged transactionObj, LCSMeasurements measurementsObj) throws WTException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.isValidTransactionObject(transactionObj, measurementsObj) ###");
		String eventType = (String) transactionObj.getValue(HBIProperties.hbiEventTypeKey);
		String measurementType = measurementsObj.getMeasurementsType();
		Collection<FlexObject> measurementComponentsCollection = null;
		boolean transactionObjectStatus = true;
		
		//Validate the given LCSMeasurements Type & transaction object event type, re-initialize transaction object status flag based on the validation of measurement type, action
		if("INSTANCE".equalsIgnoreCase(measurementType) && "create".equalsIgnoreCase(eventType))
		{
			transactionObjectStatus = false;
			measurementComponentsCollection = FlexSpecQuery.componentWhereUsed((LCSMeasurementsMaster)measurementsObj.getMaster());
		}
		
		//validating the Collection<FlexObject> (contains set of components associated with the given measurements) and initializing Season object from the components link
		if(measurementComponentsCollection != null && measurementComponentsCollection.size() > 0 )
		{
			FlexObject flexObj = measurementComponentsCollection.iterator().next();
			LCSSeason seasonObj = (LCSSeason) LCSQuery.findObjectById("VR:com.lcs.wc.season.LCSSeason:"+flexObj.getString("SPECTOLATESTITERSEASON.SEASONBRANCHID")); 
			seasonObj = (LCSSeason) VersionHelper.latestIterationOf(seasonObj);
			
			//validating the given measurement instance associated product type (data sync will happen only for Block Products as per HBI business rules) and re-initialize flag
			if(HBIProperties.hbiBlockProductsSeasonFlexType.equalsIgnoreCase(seasonObj.getFlexType().getFullName(true)))
			{
				transactionObjectStatus = true;
			}
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.isValidTransactionObject(transactionObj, measurementsObj) ###");
		return transactionObjectStatus;
	}
	
	/**
	 * This function is using to validate the given LCSPointsOfMeasure dropped status(delete status) and LCSPointsOfMeasure parent(LCSMeasurements) dropped status(delete status)
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @param transactionObj - LCSLifecycleManaged
	 * @return transactionObjectComments - String
	 * @throws WTException
	 */
	public String isValidPointsOfMeasureTransactionObject(LCSLifecycleManaged transactionObj, LCSPointsOfMeasure pointsOfMeasureObj) throws WTException
	{
		// LCSLog.debug("### START HBIIntegrationClientUtil.isValidPointsOfMeasureTransactionObject(pointsOfMeasureObj) ###");
		String transactionObjectComments = "";
		String pointsOfMeasureType = pointsOfMeasureObj.getPointsOfMeasureType();
		String eventType = (String) transactionObj.getValue(HBIProperties.hbiEventTypeKey);
		
		//Validating the given LCSPointsOfMeasure dropped status(delete status) and LCSPointsOfMeasure parent(LCSMeasurements) object status(is valid object or deleted)
		if(pointsOfMeasureObj.isDropped() && "create".equalsIgnoreCase(eventType))
		{
			transactionObjectComments = "LCSPointsOfMeasure instance/object is deleted from FlexPLM 9.2 server before it is processing to FlexPLM 10.1 server to perform create event";
		}
		else if(!"LIBRARY".equalsIgnoreCase(pointsOfMeasureType))
		{
			if(pointsOfMeasureObj.getMeasurements() == null)
			{
				transactionObjectComments = "LCSPointsOfMeasure Parent(LCSMeasurements object) deleted/having issue, which is causing the Processing failure of LCSPointsOfMeasure";
			}
		}
		
		// LCSLog.debug("### END HBIIntegrationClientUtil.isValidPointsOfMeasureTransactionObject(pointsOfMeasureObj) ###");
		return transactionObjectComments;
	}
}