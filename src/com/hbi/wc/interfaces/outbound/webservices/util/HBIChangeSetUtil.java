package com.hbi.wc.interfaces.outbound.webservices.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;

import com.lcs.wc.db.FlexObject;
import com.lcs.wc.flextype.FlexType;
import com.lcs.wc.flextype.FlexTypeAttribute;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSPointsOfMeasure;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.LCSLog;

import wt.enterprise.RevisionControlled;
import wt.fc.WTObject;
import wt.org.WTUser;
import wt.util.WTException;

/**
 * HBIChangeSetUtil.java
 * 
 * This class contain generic functions which are using to get List<String> of FlexTypeAttribute-Keys for the given String(as an input parameter), validate and return change set for
 * the given WTObjects (current WTObject and previous version WTObject) and FlexTypeAttribute, which are using to determine Transaction object creation for data sync to target PLM
 * @author Abdul.Patel@Hanes.com
 * @since  March-30-2015
 */
public class HBIChangeSetUtil 
{
	/**
	 * This function is using to validate and return change set from the function header for the given WTObjects(current WTObject and old WTObject) and List of FlexTypeAttributes
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param currentObjFlexType - FlexType
	 * @param flexTypeAttributePropertyEntry - String
	 * @return changeSet - String
	 * @throws WTException
	 */
	public String populateChangeSet(WTObject currentObj, WTObject oldObj, FlexType currentObjFlexType, String flexTypeAttributePropertyEntry) throws WTException, IllegalAccessException, InvocationTargetException
	{
		LCSLog.debug("### START HBIChangeSetUtil.populateChangeSet(currentObj, oldObj, currentObjFlexType, flexTypeAttributePropertyEntry) ###");
		Method getValueMethod = new HBIIntegrationClientUtil().getFlexObjectgetValueMethod(currentObj);
		FlexTypeAttribute flexTypeAttributeObj = null;
		String flexTypeAttributeVariable = "";
		String changeSet = "";
		
		//Calling a function which will format and return List of FlexTypeAttributeKeys for the given PropertyEntryValue (which contains set of FlexTypeAttributeKeys with delimiter)
		List<String> flexTypeAttributeKeysList = new HBIIntegrationClientUtil().getFlexTypeAttributeKeysFromPropertiesFile(flexTypeAttributePropertyEntry);
		
		//Calling a function which is using to validate logical attributes(like 'Primary Image URL' from Measurements and 'Sorting Number' from PointsOfMeasure) change set status
		changeSet = populateChangeSetForLogicalAttributes(currentObj, oldObj, getValueMethod);
		
		//Validate the given current object and previous iteration object and attributes list which are using to validate and populate change set status for the given instances
		if(currentObj != null && oldObj != null && flexTypeAttributeKeysList.size() > 0 && !FormatHelper.hasContent(changeSet))
		{
			for(String flexTypeAttributeKey : flexTypeAttributeKeysList)
			{
				flexTypeAttributeObj = currentObjFlexType.getAttribute(flexTypeAttributeKey);
				flexTypeAttributeVariable = flexTypeAttributeObj.getVariableName();
				
				//This block of code is for FlexTypeAttributes of type ATT, get FlexTypeAttribute value from current WTObject and old WTObject, validate and return data change set
				if(flexTypeAttributeVariable.contains("att"))
				{
					changeSet = populateChangeSetForATTAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey);
				}
				//This block of code is for FlexTypeAttributes of type DATE, get FlexTypeAttribute value from current WTObject and old WTObject, validate and return data change set
				else if(flexTypeAttributeVariable.contains("date"))
				{
					changeSet = populateChangeSetForDATEAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey);		
				}
				//This block of code is for FlexTypeAttributes of type NUM, get FlexTypeAttribute value from current WTObject and old WTObject, validate and return data change set
				else if(flexTypeAttributeVariable.contains("num"))
				{
					changeSet = populateChangeSetForNUMAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey, flexTypeAttributeObj);
				}
				
				//Validate the given change set(which contains FlexTypeAttributeKey) and return from the function header to capture change set on Transaction object creation
				if(FormatHelper.hasContent(changeSet))
					return changeSet;
			}
		}
		
		LCSLog.debug("### END HBIChangeSetUtil.populateChangeSet(currentObj, oldObj, currentObjFlexType, flexTypeAttributePropertyEntry) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate and return change set for the given WTObject(current and previous version) and FlexTypeAttribute (variable type of the attribute is ATT)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @param flexTypeAttributeKey - String
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForATTAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod, String flexTypeAttributeKey) throws WTException, IllegalAccessException, InvocationTargetException
	{
		//LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForATTAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		String changeSet = "";
		
		//Get FlexTypeAttribute Values(of String type) from the given object(current version and previous version of the WTObject), using to validate and return change set status
		String currentObjectStrValue = ""+(String) getValueMethod.invoke(currentObj, flexTypeAttributeKey);
		String previousObjectStrValue = ""+(String) getValueMethod.invoke(oldObj, flexTypeAttributeKey);
		
		//Validating the change set(comparing Attribute value from the given current and previous version of the WTObject), based on the validation returning the status
		if(!currentObjectStrValue.equalsIgnoreCase(previousObjectStrValue))
		{
			changeSet = flexTypeAttributeKey;
		}
		
		//LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForATTAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate and return change set for the given WTObject(current and previous version) and FlexTypeAttribute (variable type of the attribute is DATE)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @param flexTypeAttributeKey - String
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForDATEAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod, String flexTypeAttributeKey) throws WTException, IllegalAccessException, InvocationTargetException
	{
		//LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForDATEAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		String changeSet = "";
		
		//Get FlexTypeAttribute Values(of Date type) from the given object(current version and previous version of the WTObject), using to validate and return change set status
		Timestamp currentObjectTsValue = (Timestamp)getValueMethod.invoke(currentObj, flexTypeAttributeKey);
		Timestamp previousObjectTsValue = (Timestamp)getValueMethod.invoke(oldObj, flexTypeAttributeKey);
		
		//Validating the change set(comparing Attribute value from the given current and previous version of the WTObject), based on the validation returning the status
		if(currentObjectTsValue != null && (!currentObjectTsValue.equals(previousObjectTsValue)))
		{
			changeSet = flexTypeAttributeKey;
		}
		else if(currentObjectTsValue != previousObjectTsValue)
		{
			changeSet = flexTypeAttributeKey;
		}
		
		//LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForDATEAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate and return change set for the given WTObject(current and previous version) and FlexTypeAttribute (variable type of the attribute is NUM)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @param flexTypeAttributeKey - String
	 * @param flexTypeAttributeObj - FlexTypeAttribute
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForNUMAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod, String flexTypeAttributeKey, FlexTypeAttribute flexTypeAttributeObj) throws WTException, IllegalAccessException, InvocationTargetException
	{
		//LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForNUMAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		String changeSet = "";
		
		//This block of code is to validate Object Reference attribute change set for the given WTObject (current version and previous version of the WTObject) and return status
		if("object_ref".equalsIgnoreCase(flexTypeAttributeObj.getAttVariableType()))
		{
			changeSet = populateChangeSetForObjectReferenceAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey);
		}
		//This block of code is to validate User List attribute change set for the given WTObject (current version and previous version of the WTObject) and return status
		else if("userList".equalsIgnoreCase(flexTypeAttributeObj.getAttVariableType()))
		{
			changeSet = populateChangeSetForUserListAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey);
		}
		//This block of code is to validate Integer/Float attribute change set for the given WTObject (current version and previous version of the WTObject) and return status
		else
		{
			double currentObjectDblValue = (Double) getValueMethod.invoke(currentObj, flexTypeAttributeKey);
			double previousObjectDblValue = (Double) getValueMethod.invoke(oldObj, flexTypeAttributeKey);
			
			//Validating the change set(comparing Attribute value from the given current and previous version of the WTObject), based on the validation returning the status
			if(currentObjectDblValue != previousObjectDblValue)
			{
				changeSet = flexTypeAttributeKey;
			}
		}
				
		//LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForNUMAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate and return change set for the given WTObject(current and previous version) & Object Reference FlexTypeAttribute(variable type object_ref)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @param flexTypeAttributeKey - String
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForObjectReferenceAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod, String flexTypeAttributeKey) throws WTException, IllegalAccessException, InvocationTargetException
	{
		//LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForObjectReferenceAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		Integer currentObjRefObjectIdentifier = 0;
		Integer oldObjRefObjectIdentifier = 0;
		String changeSet = "";
		
		//Get Object Identifier (IDA2A2 or BranchIdIterationInfo) from the Referencing Object(current version), which are using to to validate the given FlexTypeAttribute change set
		WTObject currentVersionObjReferenceObj = (WTObject) getValueMethod.invoke(currentObj, flexTypeAttributeKey);
		if(currentVersionObjReferenceObj != null && currentVersionObjReferenceObj instanceof RevisionControlled)
		{
			currentObjRefObjectIdentifier = Integer.parseInt(FormatHelper.getNumericVersionIdFromObject((RevisionControlled) currentVersionObjReferenceObj));
		}
		else if(currentVersionObjReferenceObj != null)
		{
			currentObjRefObjectIdentifier = Integer.parseInt(FormatHelper.getNumericObjectIdFromObject(currentVersionObjReferenceObj));
		}
		
		//Get Object Identifier (IDA2A2 or BranchIdIterationInfo) from the Referencing Object(old version), which are using to to validate the given FlexTypeAttribute change set
		WTObject oldVersionObjReferenceObj = (WTObject) getValueMethod.invoke(oldObj, flexTypeAttributeKey);
		if(oldVersionObjReferenceObj != null && oldVersionObjReferenceObj instanceof RevisionControlled)
		{
			oldObjRefObjectIdentifier = Integer.parseInt(FormatHelper.getNumericVersionIdFromObject((RevisionControlled) oldVersionObjReferenceObj));
		}
		else if(oldVersionObjReferenceObj != null)
		{
			oldObjRefObjectIdentifier = Integer.parseInt(FormatHelper.getNumericObjectIdFromObject(oldVersionObjReferenceObj));
		}
		
		//Validating the change set(comparing Attribute value from the given current and previous version of the WTObject), based on the validation returning the status
		if(currentObjRefObjectIdentifier != oldObjRefObjectIdentifier)
		{
			changeSet = flexTypeAttributeKey;
		}
		
		//LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForObjectReferenceAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate and return change set for the given WTObject(current and previous version) and User List FlexTypeAttribute(variable identifier is userList)
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @param flexTypeAttributeKey - String
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForUserListAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod, String flexTypeAttributeKey) throws WTException, IllegalAccessException, InvocationTargetException
	{
		//LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForUserListAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		WTUser currentFlexObjUserObj = null;
		WTUser oldFlexObjUserObj = null;
		String changeSet = "";
		
		//Get FlexObject(which is an instanceof WTUser) from the given current object(which is an instanceof WTObject), which is using to validate and return change set status
		FlexObject currentObjUserListObj = (FlexObject) getValueMethod.invoke(currentObj, flexTypeAttributeKey);
		if (currentObjUserListObj != null)
		{
			currentFlexObjUserObj = (WTUser) LCSQuery.findObjectById("wt.org.WTUser:" + (String) ((FlexObject) currentObjUserListObj).get("OID"));
		}
		
		//Get FlexObject(which is an instanceof WTUser) from the given old object(which is an instanceof WTObject), which is using to validate and return change set status
		FlexObject oldObjUserListObj = (FlexObject) getValueMethod.invoke(oldObj, flexTypeAttributeKey);
		if (oldObjUserListObj != null)
		{
			oldFlexObjUserObj = (WTUser) LCSQuery.findObjectById("wt.org.WTUser:" + (String) ((FlexObject) oldObjUserListObj).get("OID"));
		}
		
		//Validating the change set(comparing Attribute value from the given current and previous version of the WTObject), based on the validation returning the status
		if(currentFlexObjUserObj != oldFlexObjUserObj)
		{
			changeSet = flexTypeAttributeKey;
		}
		
		//LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForUserListAttributes(currentObj, oldObj, getValueMethod, flexTypeAttributeKey) ###");
		return changeSet;
	}
	
	/**
	 * This function is using to validate logical attributes(like 'Primary Image URL' from Measurements and 'Sorting Number' from PointsOfMeasure) change set using in transaction BO 
	 * @param currentObj - WTObject
	 * @param oldObj - WTObject
	 * @param getValueMethod - Method
	 * @return changeSet - String
	 * @throws WTException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private String populateChangeSetForLogicalAttributes(WTObject currentObj, WTObject oldObj, Method getValueMethod) throws WTException, IllegalAccessException, InvocationTargetException
	{
		// LCSLog.debug("### START HBIChangeSetUtil.populateChangeSetForLogicalAttributes(currentObj, oldObj, getValueMethod) ###");
		String changeSet = "";
		
		//Validating measurements image change set from the given object(current version and previous version of the WTObject), which is using to validate and return change set status
		if(currentObj instanceof LCSMeasurements && oldObj instanceof LCSMeasurements)
		{
			String currentObjImageURL = ""+((LCSMeasurements) currentObj).getPrimaryImageURL();
			String oldObjImageURL = ""+((LCSMeasurements) oldObj).getPrimaryImageURL();
			if(!currentObjImageURL.equalsIgnoreCase(oldObjImageURL))
			{
				changeSet = "Measurements_Image";
			}
		}
		//Validating sorting number change set from the given object(current version and previous version of the WTObject), which is using to validate and return change set status
		else if(currentObj instanceof LCSPointsOfMeasure && oldObj instanceof LCSPointsOfMeasure)
		{
			LCSPointsOfMeasure currentPOMObj = (LCSPointsOfMeasure) currentObj;
			LCSPointsOfMeasure oldPOMObj = (LCSPointsOfMeasure) oldObj;
			if(currentPOMObj.getSortingNumber() != oldPOMObj.getSortingNumber())
			{
				changeSet = "Sorting_Order";
			}
		}
		
		// LCSLog.debug("### END HBIChangeSetUtil.populateChangeSetForLogicalAttributes(currentObj, oldObj, getValueMethod) ###");
		return changeSet;
	}
}