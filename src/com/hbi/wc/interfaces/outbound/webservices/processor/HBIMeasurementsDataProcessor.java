package com.hbi.wc.interfaces.outbound.webservices.processor;

import java.rmi.RemoteException;
import java.util.Collection;

import com.hbi.wc.interfaces.outbound.webservices.measurements.ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean;
import com.hbi.wc.interfaces.outbound.webservices.measurements.IESoapServletLocator;
import com.hbi.wc.interfaces.outbound.webservices.measurements.SoapBindingStub;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIIntegrationClientUtil;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIProperties;
import com.lcs.wc.db.FlexObject;
import com.lcs.wc.foundation.LCSLifecycleManaged;
import com.lcs.wc.foundation.LCSLifecycleManagedHelper;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSMeasurementsMaster;
import com.lcs.wc.product.LCSProduct;
import com.lcs.wc.season.LCSSeason;
import com.lcs.wc.sizing.FullSizeRange;
import com.lcs.wc.sizing.SizeCategory;
import com.lcs.wc.sourcing.LCSSourcingConfig;
import com.lcs.wc.specification.FlexSpecQuery;
import com.lcs.wc.specification.FlexSpecification;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.LCSLog;
import com.lcs.wc.util.LCSProperties;
import com.lcs.wc.util.VersionHelper;

import wt.method.MethodContext;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.session.SessionContext;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * HBIMeasurementsDataProcessor.java
 * 
 * This class is using as a Processor(which will invoke data sync tool from source to target system) to process all pending transactions & perform different actions in target server
 * @author Abdul.Patel@Hanes.com
 * @since  April-8-2015
 */
public class HBIMeasurementsDataProcessor implements RemoteAccess
{
	private static String CLIENT_ADMIN_USER_ID = "";
	private static String CLIENT_ADMIN_PASSWORD = "";
	private static String SERVER_ADMIN_USER_ID = "";
	private static String SERVER_ADMIN_PASSWORD = "";
	private static RemoteMethodServer remoteMethodServer;
	
	static
	{
		try
		{
			CLIENT_ADMIN_USER_ID = LCSProperties.get("com.hbi.wc.integration.CLIENT_ADMIN_USER_ID", "Administrator");
			CLIENT_ADMIN_PASSWORD = LCSProperties.get("com.hbi.wc.integration.CLIENT_ADMIN_PASSWORD", "admin1");
			SERVER_ADMIN_USER_ID = LCSProperties.get("com.hbi.wc.integration.SERVER_ADMIN_USER_ID", "flexadmin");
			SERVER_ADMIN_PASSWORD = LCSProperties.get("com.hbi.wc.integration.SERVER_ADMIN_PASSWORD", "c9admin");
		}
		catch (Exception exp)
		{
			LCSLog.debug("Exception in static block of the class HBIMeasurementsDataProcessor is :: " + exp);
		}
	}
	
	/** Default executable function of the class HBIMeasurementsDataProcessor */
	public static void main(String[] args)
	{
		LCSLog.debug("### START HBIMeasurementsDataProcessor.main() ###");
		try
		{
			MethodContext mcontext = new MethodContext((String) null, (Object) null);
			SessionContext sessioncontext = SessionContext.newContext();

			remoteMethodServer = RemoteMethodServer.getDefault();
			remoteMethodServer.setUserName(CLIENT_ADMIN_USER_ID);
			remoteMethodServer.setPassword(CLIENT_ADMIN_PASSWORD);
			
			//remoteMethodServer.invoke("invokeMeasurementsDataProcessor", "com.hbi.wc.interfaces.outbound.webservices.processor.HBIMeasurementsDataProcessor", null, null, null);
			
			//calling a function which will validate and sync Measurements(Template and Instance) data from source FlexPLM to target FlexPLM based on the user actions on source PLM
			invokeMeasurementsDataProcessor();
			System.exit(0);
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			System.exit(1);
		}
		
		LCSLog.debug("### END HBIMeasurementsDataProcessor.main() ###");
	}
	
	/**
	 * This function is using to initialize Service Locator and invoke SOAP Protocol with java bean(contains all attributes data) to perform create or update action in target server
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void invokeMeasurementsDataProcessor() throws WTException, WTPropertyVetoException
	{
		LCSLog.debug("### START HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor() ###");
		String flexObjectClassName = "com.lcs.wc.measurements.LCSMeasurements";
		LCSLifecycleManaged transactionObj = null;
		
		try
		{
			//Initializing Service Locator and invoking SOAP Protocol with server details, preparing input bean object & calling invoke function to perform action in target server
			IESoapServletLocator serviceLocator = new IESoapServletLocator();
			SoapBindingStub stub = (SoapBindingStub) serviceLocator.getIESoapPort();
			stub.setUsername(SERVER_ADMIN_USER_ID);
			stub.setPassword(SERVER_ADMIN_PASSWORD);
			stub._setProperty("sendMultiRefs", new Boolean(false));
			
			//Calling a function to get 'Pending' Transaction object from the pre-defined 'Business Object' path for the given FlexObject class name and other internal parameters
			Collection<FlexObject> hbiTransactionObjectCollection = new HBIIntegrationClientUtil().getHBITransactionBusinessObjectForPendingStatus(flexObjectClassName);
			for(FlexObject flexObj : hbiTransactionObjectCollection)
			{
				transactionObj = (LCSLifecycleManaged) LCSQuery.findObjectById("OR:com.lcs.wc.foundation.LCSLifecycleManaged:"+ flexObj.getString("LCSLIFECYCLEMANAGED.IDA2A2"));
				if(transactionObj != null)
				{
					//Calling a function to initialize LCSMeasurements from the given Transaction object, prepare java bean from the LCSMeasurements object and invoke stub function
					new HBIMeasurementsDataProcessor().invokeMeasurementsDataProcessor(stub, transactionObj);
				}
			}
		}
		catch (Exception exp)
		{
			//Updating the given transactionObj(instance of LCSLifecycleManaged) for 'Comments' field to provide the reason for not processing the Transaction successfully
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "Manual Intervention needed:- Exception in Processing Transaction object is :: "+ exp.getMessage());
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "failed");
			LCSLifecycleManagedHelper.getService().saveLifecycleManaged(transactionObj);
			exp.printStackTrace();
		}
		
		LCSLog.debug("### END HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor() ###");
	}
	
	/**
	 * This function is using to initialize LCSMeasurements from the given LCSLifecycleManaged instance, prepare java bean from the LCSMeasurements object and invoke stub function
	 * @param stub - SoapBindingStub
	 * @param transactionObj - LCSLifecycleManaged
	 * @return transactionObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 * @throws RemoteException
	 */
	public LCSLifecycleManaged invokeMeasurementsDataProcessor(SoapBindingStub stub, LCSLifecycleManaged transactionObj) throws WTException, WTPropertyVetoException, RemoteException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor(transactionObj) ###");
		
		//Get FlexObject Unique Identifier from the given Transaction object, format LCSMeasurements instance which is using to populate the java beans data object
		String flexObjectOID = (String) transactionObj.getValue(HBIProperties.hbiFlexObjectOIDKey);
		LCSMeasurements measurementsObj = (LCSMeasurements) LCSQuery.findObjectById(flexObjectOID);
		measurementsObj = (LCSMeasurements) VersionHelper.latestIterationOf(measurementsObj);
		
		//Validating the given object, invoking various internal functions for validation and data preparation, invoke stub function for data sync and update transaction object
		if(measurementsObj != null)
		{
			//Calling a function get the transaction object status(is valid transaction object to invoke data sync process) and java beans instance with all attributes data set
			boolean isvalidTransactionObject = new HBIIntegrationClientUtil().isValidTransactionObject(transactionObj, measurementsObj);
			if(isvalidTransactionObject)
			{
				ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean measurementsBeanObj = invokeMeasurementsDataProcessor(measurementsObj);
				stub.hbiMeasurementsTask(measurementsBeanObj);
			}
			else
			{
				transactionObj.setValue(HBIProperties.hbiCommentsKey, "This is a dummy transaction object created for Measurements instnace data which is other than Block Products, hence marking as completed without processing to VRD");
			}
			
			//Updating the given transactionObj(instance of LCSLifecycleManaged) to mark 'Transaction Status' as 'Completed' as this is Transaction is successfully processed
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "completed");
			LCSLifecycleManagedHelper.getService().saveLifecycleManaged(transactionObj);
		}
		else
		{
			//Updating the given transactionObj(instance of LCSLifecycleManaged) for 'Comments' field to provide the reason for not processing the Transaction successfully
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "Manual Intervention needed:- Issue in forming LCSMeasurements object for the given OID :: "+ flexObjectOID);
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "failed");
			LCSLifecycleManagedHelper.getService().saveLifecycleManaged(transactionObj);
		}
		
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor(transactionObj) ###");
		return transactionObj;
	}
	
	/**
	 * This function is using to initialize 'Measurement Bean' instance and populate bean data with the given LCSMeasurements instance and return bean object to the calling function
	 * @param measurementsObj - LCSMeasurements
	 * @return measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @throws WTException
	 */
	public ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean invokeMeasurementsDataProcessor(LCSMeasurements measurementsObj) throws WTException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor(measurementsObj) ###");
		String measurementsType = measurementsObj.getMeasurementsType();
		
		//Get Attribute data('Measurement Name', 'Measurement Type' and 'UOM') from the given LCSMeasurements object, which is using to update to the entity beans object
		String measurementsName = getMeasurementsName(measurementsObj, measurementsType);
		String primaryImageURL = ""+measurementsObj.getPrimaryImageURL();
		String uom = (String) measurementsObj.getValue(HBIProperties.uomKey);
		
		//Initialize MeasurementBean object and update 'Measurement Name', 'Measurement Type' and 'UOM'(within the context) to the bean, using in stub invoke function
		ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean measurementsBeanObj = new ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean();
		measurementsBeanObj.setMeasurementsName(measurementsName);
		measurementsBeanObj.setInstanceType(measurementsType);
		measurementsBeanObj.setUom(uom);
		measurementsBeanObj.setPrimaryImageURL(primaryImageURL);
		
		//Validate the 'Measurement Type' and invoke the corresponding function with 'measurementsBeanObj' and 'measurementsObj' to update entity bean & return to a calling function
		if("GRADINGS".equalsIgnoreCase(measurementsType))
		{
			measurementsBeanObj = updateMeasurementsBeanForGradeRuleData(measurementsBeanObj, measurementsObj);
		}
		//Validate the 'Measurement Type' and invoke the corresponding function with 'measurementsBeanObj' and 'measurementsObj' to update entity bean & return to a calling function
		else if("INSTANCE".equalsIgnoreCase(measurementsType))
		{
			measurementsBeanObj = updateMeasurementsBeanForInstanceData(measurementsBeanObj, measurementsObj);
		}
		
		//REMOVE:- Added only for Unit testing in order to test the update of LCSMeasurements, remove/comment-out this block of code while deploying the functionality in Production
		String test1 = (String) measurementsObj.getValue("test1");
		measurementsBeanObj.setTest1(test1);
		
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.invokeMeasurementsDataProcessor(measurementsObj) ###");
		return measurementsBeanObj;
	}
	
	/**
	 * This function is using to update the given Measurement Bean Attribute data(data for Grading Method, Size Category and FullSizeRange) from the given LCSMeasurements instance
	 * @param measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @param measurementsObj - LCSMeasurements
	 * @return measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @throws WTException
	 */
	public ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean updateMeasurementsBeanForGradeRuleData(ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean measurementsBeanObj, LCSMeasurements measurementsObj) throws WTException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.updateMeasurementsBeanForGradeRuleData(measurementsBeanObj, measurementsObj) ###");
		String gradingMethod = ""+(String) measurementsObj.getValue(HBIProperties.gradingMethodKey);
		measurementsBeanObj.setGradingMethod(gradingMethod);
		
		//Get SizeCategory from the given LCSMeasurements, validate the SizeCategory and get SizeCategory Name which is needed to update on the given measurementsBeanObj
		SizeCategory sizeCategoryObj = measurementsObj.getSizeCategory();
		if(sizeCategoryObj != null)
		{
			String sizeCategoryName = ""+sizeCategoryObj.getName();
			measurementsBeanObj.setSizeCategoryName(sizeCategoryName);
		}
		
		//Get FullSizeRange from the given LCSMeasurements, validate the FullSizeRange and get FullSizeRange Name, Base Size and Size Values, needed to update on the given beanObj
		FullSizeRange fullSizeRangeObj = measurementsObj.getFullSizeRange();
		if(fullSizeRangeObj != null)
		{
			String fullSizeRangeName = ""+fullSizeRangeObj.getName();
			String baseSize = ""+fullSizeRangeObj.getBaseSize();
			String sizeValues = ""+fullSizeRangeObj.getSizeValues();
			measurementsBeanObj.setFullSizeRangeName(fullSizeRangeName);
			measurementsBeanObj.setBaseSize(baseSize);
			measurementsBeanObj.setSizeValues(sizeValues);
		}
		
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.updateMeasurementsBeanForGradeRuleData(measurementsBeanObj, measurementsObj) ###");
		return measurementsBeanObj;
	}
	
	/**
	 * This function is using to update the given Measurement Bean Attribute data(data for Season, Product, SourcingConfig & FlexSpecification) from the given LCSMeasurements instance
	 * @param measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @param measurementsObj - LCSMeasurements
	 * @return measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @throws WTException
	 */
	@SuppressWarnings("unchecked")
	public ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean updateMeasurementsBeanForInstanceData(ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean measurementsBeanObj, LCSMeasurements measurementsObj) throws WTException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.updateMeasurementsBeanForInstanceData(measurementsBeanObj, measurementsObj) ###");
		Collection<FlexObject> measurementComponentsCollection = FlexSpecQuery.componentWhereUsed((LCSMeasurementsMaster)measurementsObj.getMaster());
		
		//validating the Collection<FlexObject> (contains set of components associated with the given measurements) and initializing component object needed for measurements bean
		if(measurementComponentsCollection != null && measurementComponentsCollection.size() > 0 )
		{
			FlexObject flexObj = measurementComponentsCollection.iterator().next();
			
			//Forming LCSSeason from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSSeason seasonObj = (LCSSeason) LCSQuery.findObjectById("VR:com.lcs.wc.season.LCSSeason:"+flexObj.getString("SPECTOLATESTITERSEASON.SEASONBRANCHID")); 
			seasonObj = (LCSSeason) VersionHelper.latestIterationOf(seasonObj);
			measurementsBeanObj.setSeasonName(seasonObj.getName());
			
			//Forming LCSProduct from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSProduct productObj = (LCSProduct) LCSQuery.findObjectById("VR:com.lcs.wc.product.LCSProduct:"+flexObj.getString("LCSPRODUCT.BRANCHIDITERATIONINFO"));
			productObj = (LCSProduct) VersionHelper.latestIterationOf(productObj);
			measurementsBeanObj.setProductName(productObj.getName());
			measurementsBeanObj.setHbiPatternNo((String) productObj.getValue(HBIProperties.hbiPatternNoKey));
			
			//Forming SourcingConfig from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSSourcingConfig sourcingConfigObj = (LCSSourcingConfig) LCSQuery.findObjectById("VR:com.lcs.wc.sourcing.LCSSourcingConfig:"+flexObj.getString("LCSSOURCINGCONFIG.BRANCHIDITERATIONINFO"));
			sourcingConfigObj = (LCSSourcingConfig) VersionHelper.latestIterationOf(sourcingConfigObj);
			measurementsBeanObj.setSourcingConfigName(sourcingConfigObj.getSourcingConfigName());
			
			//Forming FlexSpecification from 'Measurements' Instance data(get all Components from the given Measurement), using as a parameter to process requested action in VRD
			FlexSpecification flexSpecObj = (FlexSpecification) LCSQuery.findObjectById("VR:com.lcs.wc.specification.FlexSpecification:"+flexObj.getString("LATESTITERFLEXSPECIFICATION.BRANCHIDITERATIONINFO"));
			flexSpecObj = (FlexSpecification) VersionHelper.latestIterationOf(flexSpecObj);
			measurementsBeanObj.setSpecificationName(flexSpecObj.getName());
			
			//Calling a function to update product measurement reference details(template measurement, grading rule, size category & full size range) to the given measurement bean
			measurementsBeanObj = updateMeasurementsBeanForInstanceDataReferences(measurementsBeanObj, measurementsObj);
		}
		
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.updateMeasurementsBeanForInstanceData(measurementsBeanObj, measurementsObj) ###");
		return measurementsBeanObj;
	}
	
	/**
	 * This function is using to update product measurement reference details(template measurement, grading rule, size category & full size range) to the given measurement bean object
	 * @param measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @param measurementsObj - LCSMeasurements
	 * @return measurementsBeanObj - ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean
	 * @throws WTException
	 */
	public ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean updateMeasurementsBeanForInstanceDataReferences(ComHbiWcInterfacesOutboundWebservicesMeasurementsHBIMeasurementsBean measurementsBeanObj, LCSMeasurements measurementsObj) throws WTException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.updateMeasurementsBeanForInstanceDataReferences(measurementsBeanObj, measurementsObj) ###");
		
		//Get Measurement Template from the given Measurement instance object, get measurement template name and update to the given SOAP request bean object
		LCSMeasurementsMaster sourceTemplateMasterObj = measurementsObj.getSourceTemplate();
		if(sourceTemplateMasterObj != null)
		{
			LCSMeasurements sourceTemplateObj = (LCSMeasurements) VersionHelper.latestIterationOf(sourceTemplateMasterObj);
			measurementsBeanObj.setMeasurementsTemplateName(sourceTemplateObj.getName());
		}
		
		//Get Measurement Grading Rule from the given Measurement instance object, get measurement grading rule name and update to the given SOAP request bean object
		LCSMeasurementsMaster measurementsGradeRuleMasterObj = measurementsObj.getGradings();
		if(measurementsGradeRuleMasterObj != null)
		{
			LCSMeasurements measurementsGradeRuleObj = (LCSMeasurements) VersionHelper.latestIterationOf(measurementsGradeRuleMasterObj);
			measurementsBeanObj.setMeasurementsGradeRule(measurementsGradeRuleObj.getName());
			
			//Calling a function to update the given Measurement Bean Attribute data(data for Grading Method, Size Category and FullSizeRange) from the given LCSMeasurements instance
			measurementsBeanObj = updateMeasurementsBeanForGradeRuleData(measurementsBeanObj, measurementsObj);
		}
		
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.updateMeasurementsBeanForInstanceDataReferences(measurementsBeanObj, measurementsObj) ###");
		return measurementsBeanObj;
	}
	
	/**
	 * This function is using to Validate the Measurement Type(instance) and format the Measurement object name (instance measurement object contains(appended) sequence number in name)
	 * @param measurementsObj - LCSMeasurements
	 * @param measurementsType - String
	 * @return measurementsName - String
	 * @throws WTException
	 */
	public String getMeasurementsName(LCSMeasurements measurementsObj, String measurementsType) throws WTException
	{
		// LCSLog.debug("### START HBIMeasurementsDataProcessor.getMeasurementsName(measurementsObj, measurementsType) ###");
		String measurementsName = measurementsObj.getMeasurementsName();
		
		//Validate the Measurement Type(to check the type like 'Template', 'Library', "Instance') and format the given Measurement object name for Instance measurement types
		if("INSTANCE".equalsIgnoreCase(measurementsType))
		{
			if(FormatHelper.hasContent(measurementsName) && measurementsName.contains(":"))
			{
				String measurementsNameArray[] = measurementsName.split(":");
				measurementsName = measurementsNameArray[1].trim();
			}
		}
		
		return measurementsName;
		// LCSLog.debug("### END HBIMeasurementsDataProcessor.getMeasurementsName(measurementsObj, measurementsType) ###");
	}
}