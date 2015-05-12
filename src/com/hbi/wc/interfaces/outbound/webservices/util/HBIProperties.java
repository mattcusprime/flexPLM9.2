package com.hbi.wc.interfaces.outbound.webservices.util;

import com.lcs.wc.util.LCSProperties;

/**
 * HBIProperties.java
 * 
 * This class contains static references( such as FlexTypeAttribute Keys, Display Names, FlexType Id Paths) similar to the property entries, which are referring in various functions
 * @author Abdul.Patel@Hanes.com
 * @since  March-26-2015
 */
public class HBIProperties
{
	//Entries added to register FlexTypes for Business Object, Measurements, Size Definition, Season, Product, SourcingConfig and Specification
	public static String hbiBlockProductsSeasonFlexType = "Season\\Pattern\\Reference";
	public static String hbiBlockProductsProductFlexType = "Product\\BASIC CUT & SEW - PATTERN";
	public static String hbiBlockProductsSourcingConfigFlexType = "Sourcing Configuration\\Pattern";
	public static String hbiBlockProductsSpecificationFlexType = "Specification\\Basic Cut and Sew - Pattern";
	public static String hbiTransactionBOFlexType = "Business Object\\Integration\\Outbound\\Transaction BO";
	public static String measurementsFlexType = "Measurements";
	public static String sizeDefinitionFlexType = "Size Definition";
	
	//Entries added for Out-bound Integration Transaction Object Fields(data flow from FlexPLM 9.2 to FlexPLM 10.1)  
	public static String hbiTransactionStatusKey = "hbiIntegrationStatus";
	public static String hbiCommentsKey = "hbiComments";
	public static String hbiEventTriggeredByKey = "hbiEventTriggeredBy";
	public static String hbiFlexBranchIdKey = "hbiFlexBranchId";
	public static String hbiFlexObjectIdKey = "hbiFlexObjectId";
	public static String hbiFlexObjectClassNameKey = "hbiFlexObjectClassName";
	public static String hbiFlexObjectNameKey = "hbiFlexObjectName";
	public static String hbiFlexObjectOIDKey = "hbiFlexObjectOID";
	public static String hbiTransactionIdKey = "hbiTransactionId";
	public static String hbiEventTypeKey = "hbiEventType";
	public static String hbiGradeCodeKey = "hbiGradeCode";
	public static String hbiChangeSetKey = "hbiChangeSet";
	public static String hbiFlexObjectUniqueIdentifierKey = "hbiFlexObjectUniqueIdentifier";
	
	public static String hbiTransactionBOFetchLimit = LCSProperties.get("com.hbi.wc.interfaces.outbound.webservices.util.HBIIntegrationUtil.hbiTransactionBOFetchLimit", "5000");
	
	//Entries added to register FlexTypeAttribute-Keys for Measurements Out-bound Integration (data flow from FlexPLM 9.2 to FlexPLM 10.1)
	public static String measurementsMeasurementScopeAttributes = "htmInstruction,hbiGradeCode,measurementName,newMeasurement,placementAmount,placementReference,number,minusTolerance,plusTolerance,sampleMeasurementComments,howToMeasure";
	public static String measurementsProductScopeAttributes = "uom,test1";
	
	//Entries added to register FlexTypeAttribute-Keys defined at Measurements-Product Scope
	public static String measurementsNameKey = "measurementsName";
	public static String uomKey = "uom";
	public static String gradingMethodKey = "gradingMethod";
	
	//Entries added to register FlexTypeAttribute-Keys defined at Measurements-Measurement(POM) Scope
	public static String htmInstructionKey = "htmInstruction";
	public static String sampleMeasurementCommentsKey = "sampleMeasurementComments";
	public static String measurementNameKey = "measurementName";
	public static String newMeasurementKey = "newMeasurement";
	public static String placementAmountKey = "placementAmount";
	public static String placementReferenceKey = "placementReference";
	public static String numberKey = "number";
	public static String minusToleranceKey = "minusTolerance";
	public static String plusToleranceKey = "plusTolerance";
	public static String hbiPOMIntegrationIdentifierKey = "hbiPOMIntegrationIdentifier";
	public static String howToMeasureKey = "howToMeasure";
	
	//Entries added to register FlexTypeAttribute-Keys defined at Product-Product Scope
	public static String hbiPatternNoKey = "hbiPatternNo";
	
}